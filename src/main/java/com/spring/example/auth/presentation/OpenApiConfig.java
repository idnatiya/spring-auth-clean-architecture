package com.spring.example.auth.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("Example API").version("v1")
				.description("Stateless auth with JWT. Log in first, then press Authorize and paste the access token."))
			.components(new Components().addSecuritySchemes(BEARER_SCHEME,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}
}
