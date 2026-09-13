package com.spring.example.auth.domain;

import com.spring.example.auth.domain.TokenIssuer.IssuedToken;

public record AuthTokens(IssuedToken access, String refreshToken, long refreshExpiresIn) {
}
