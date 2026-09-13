# example

API autentikasi stateless dengan Spring Boot 4: registrasi, login JWT, refresh token yang dirotasi,
rate limit, dan pipeline observability (log + trace) yang jalan dari `docker compose` satu perintah.

## Jalan dalam 3 langkah

```bash
cp .env.example .env          # sesuaikan DB_PASSWORD dan JWT_SECRET
docker compose up -d          # Postgres, Redis, Alloy, Loki, Tempo, Grafana
./mvnw spring-boot:run
```

Postgres dan Redis sebenarnya dinyalakan otomatis oleh `spring-boot-docker-compose` saat
`spring-boot:run`. `docker compose up -d` tetap perlu dijalankan lebih dulu kalau kamu mau
menjalankan test, atau ingin stack observability ikut hidup.

Cek cepat:

```bash
U=andi
curl -s -XPOST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d "{\"username\":\"$U\",\"password\":\"rahasia123\"}"

TOKEN=$(curl -s -XPOST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"$U\",\"password\":\"rahasia123\"}" | jq -r .accessToken)

curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

Dokumentasi API interaktif: <http://localhost:8080/swagger-ui.html> — login dulu, salin
`accessToken`, tekan **Authorize**, tempel tokennya (tanpa kata `Bearer`).

## Endpoint

| Method | Path | Auth | Keterangan |
|---|---|---|---|
| POST | `/api/auth/register` | — | 201. Username dinormalisasi (trim + lowercase), password di-hash BCrypt |
| POST | `/api/auth/login` | — | Mengembalikan `accessToken` (15 menit) + `refreshToken` (30 hari) |
| POST | `/api/auth/refresh` | — | Menukar refresh token dengan pasangan baru; token lama langsung mati |
| POST | `/api/auth/logout` | — | 204. Mencabut seluruh rantai refresh token |
| GET | `/api/me` | Bearer | Identitas dari klaim JWT |
| GET | `/actuator/health` | — | Untuk probe |

Kode error: 400 validasi, 401 kredensial/token tidak valid, 409 username sudah dipakai,
429 rate limit (disertai header `Retry-After`).

## Arsitektur

Clean architecture, empat layer di bawah `com.spring.example.auth`:

```
presentation  → controller, DTO, exception handler, konfigurasi OpenAPI
application   → use case (Register, Login, RefreshToken, Logout) + job pembersih token
domain        → entity, port (UserRepository, PasswordHasher, TokenIssuer, RateLimiter), exception
infrastructure→ adapter JPA, Redis, JWT, Spring Security
```

Arah dependensi: `presentation → application → domain ← infrastructure`. Layer `domain` tidak
mengimpor apa pun dari `org.springframework` (kecuali anotasi JPA pada entity, kompromi yang
disengaja). Aturan itu dijaga dengan `grep`, bukan ArchUnit:

```bash
grep -rn "springframework" src/main/java/com/spring/example/auth/domain/    # harus kosong
```

## Keamanan

Yang sudah terpasang:

- **Access token pendek (15 menit)**, ditandatangani HS256, diverifikasi oleh resource server
  bawaan Spring Security — termasuk validasi `iss` dan klaim `jti` di tiap token.
- **Refresh token opaque, dirotasi tiap dipakai.** Yang disimpan di DB hanya SHA-256-nya.
  Memakai ulang token yang sudah dirotasi dianggap bukti token bocor: **seluruh rantai dicabut**,
  termasuk token terbaru yang mungkin dipegang penyerang.
- **Rate limit di Redis** — 5 percobaan per menit, per username untuk login, per IP untuk register.
- **Anti timing attack**: login dengan username tak dikenal tetap menjalankan hashing dummy,
  supaya durasi respons tidak membocorkan ada atau tidaknya sebuah akun.
- **Pesan error login selalu generik**, tidak membedakan "user tidak ada" dan "password salah".
  Log internal boleh membedakan keduanya — yang dilindungi adalah informasi ke penyerang.
- **Log tidak pernah memuat password, token mentah, atau hash-nya.** Refresh token hanya dicatat
  lewat `token_family` dan 8 karakter pertama hash-nya. Ada test yang menjaga ini.

`JWT_SECRET` wajib minimal 32 byte; aplikasi sengaja gagal start kalau kosong atau terlalu pendek.

Sebelum produksi, yang masih perlu: Flyway (sekarang `ddl-auto=update`), TLS + HSTS di depan app,
`SWAGGER_UI=false`, `TRACE_SAMPLING` jauh di bawah 1.0, dan `server.forward-headers-strategy`
supaya rate limit register membaca IP klien, bukan IP proxy.

## Observability

App hanya mengenal satu endpoint OTLP; **Grafana Alloy** yang membagi log ke Loki dan trace ke Tempo.
Bentuk ini sama dengan produksi: Loki lambat atau mati tidak pernah menahan thread aplikasi.

| Alat | URL | Guna |
|---|---|---|
| Grafana | <http://localhost:3000> | Explore log & trace, datasource ter-provisioning |
| Alloy | <http://localhost:12345> | Status pipeline collector |
| Loki | <http://localhost:3100> | API log |
| Tempo | <http://localhost:3200> | API trace |

Yang membuatnya berguna:

- Setiap baris log adalah **JSON** berisi `timestamp`, `level`, `logger`, `thread`, `message`,
  `trace_id`, dan `span_id`.
- `level` adalah **label Loki terindeks**: `{service_name="example", level="WARN"}`.
- Field MDC ikut sebagai structured metadata, jadi bisa `| username="andi"` atau
  `| token_family="..."` untuk menelusuri satu rantai sesi.
- Dari baris log, tautan **TraceID** di Grafana langsung membuka trace-nya di Tempo. Trace memuat
  span query DB beserta SQL-nya.
- Noise dibuang di collector: trace `/actuator/**` dan `/swagger-ui`, span yatim dari pool koneksi,
  dan span filter chain Spring Security.

Retensi log dan trace sama-sama **7 hari**.

Saat menelusuri bug auth, nyalakan detail per langkah:

```bash
AUTH_LOG_LEVEL=DEBUG ./mvnw spring-boot:run
```

## Konfigurasi

Semua lewat `.env` (dibaca Spring maupun docker compose). Salin dari `.env.example`; `.env` tidak
ikut ter-commit. Yang paling sering disentuh:

| Variabel | Default | Keterangan |
|---|---|---|
| `JWT_SECRET` | — | **Wajib**, minimal 32 byte. `openssl rand -base64 48` |
| `JWT_TTL_MINUTES` | 15 | Umur access token |
| `REFRESH_TTL_DAYS` | 30 | Umur refresh token |
| `LOGIN_RATE_LIMIT` / `REGISTER_RATE_LIMIT` | 5 | Percobaan per menit |
| `TRACE_SAMPLING` | 1.0 | 1.0 hanya untuk lokal |
| `AUTH_LOG_LEVEL` | INFO | Setel DEBUG saat menelusuri bug |
| `SWAGGER_UI` | true | `false` di produksi |

Port host sengaja digeser supaya tidak bentrok dengan stack lain yang mungkin sudah jalan:
Postgres **5433**, Redis **6380**.

## Test

```bash
docker compose up -d   # test butuh Postgres + Redis
./mvnw test
```

10 test, semuanya menembak Postgres dan Redis sungguhan (DB `example_test` dibuat otomatis oleh
`docker/postgres/init/01-test-db.sql`). Ekspor OTLP dimatikan selama test.

Yang dijaga test: alur register/login/endpoint terproteksi, rotasi refresh token dan deteksi reuse,
rate limit, kebersihan MDC antar-request, dan tidak bocornya rahasia ke log.

## Struktur berkas

```
src/main/java/com/spring/example/
├── auth/{domain,application,infrastructure,presentation}
└── observability/         # pemasang appender OpenTelemetry
docker/
├── alloy/config.alloy     # pipeline collector: filter, transform, routing
├── loki/loki.yaml         # retensi 7 hari + level sebagai label
├── tempo/tempo.yaml       # retensi 7 hari
├── grafana/provisioning/  # datasource + tautan log↔trace
└── postgres/init/         # pembuatan DB test
compose.yaml               # semua image dipin versinya
```
