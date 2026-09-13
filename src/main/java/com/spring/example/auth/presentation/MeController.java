package com.spring.example.auth.presentation;

import com.spring.example.auth.presentation.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
class MeController {

	@GetMapping("/api/me")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new UserResponse(jwt.getClaim("uid"), jwt.getSubject());
	}
}
