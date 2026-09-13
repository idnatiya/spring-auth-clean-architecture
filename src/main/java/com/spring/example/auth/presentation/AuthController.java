package com.spring.example.auth.presentation;

import com.spring.example.auth.application.LoginUseCase;
import com.spring.example.auth.application.LogoutUseCase;
import com.spring.example.auth.application.RefreshTokenUseCase;
import com.spring.example.auth.application.RegisterUserUseCase;
import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.presentation.AuthDtos.LoginRequest;
import com.spring.example.auth.presentation.AuthDtos.RefreshRequest;
import com.spring.example.auth.presentation.AuthDtos.RegisterRequest;
import com.spring.example.auth.presentation.AuthDtos.TokenResponse;
import com.spring.example.auth.presentation.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final RegisterUserUseCase registerUser;

	private final LoginUseCase login;

	private final RefreshTokenUseCase refreshToken;

	private final LogoutUseCase logout;

	AuthController(RegisterUserUseCase registerUser, LoginUseCase login, RefreshTokenUseCase refreshToken,
			LogoutUseCase logout) {
		this.registerUser = registerUser;
		this.login = login;
		this.refreshToken = refreshToken;
		this.logout = logout;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		User user = registerUser.execute(request.username(), request.password(), http.getRemoteAddr());
		return new UserResponse(user.getId(), user.getUsername());
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return toResponse(login.execute(request.username(), request.password()));
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return toResponse(refreshToken.execute(request.refreshToken()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody RefreshRequest request) {
		logout.execute(request.refreshToken());
	}

	private TokenResponse toResponse(AuthTokens tokens) {
		return new TokenResponse(tokens.access().value(), tokens.access().type(), tokens.access().expiresIn(),
				tokens.refreshToken(), tokens.refreshExpiresIn());
	}
}
