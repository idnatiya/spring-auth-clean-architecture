package com.spring.example.auth.presentation;

import com.spring.example.auth.domain.InvalidCredentialsException;
import com.spring.example.auth.domain.InvalidRefreshTokenException;
import com.spring.example.auth.domain.TooManyAttemptsException;
import com.spring.example.auth.domain.UsernameTakenException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthExceptionHandler {

	@ExceptionHandler(UsernameTakenException.class)
	ResponseEntity<Map<String, String>> handleTaken(UsernameTakenException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<Map<String, String>> handleBadCredentials(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ResponseEntity<Map<String, String>> handleInvalidRefresh(InvalidRefreshTokenException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(TooManyAttemptsException.class)
	ResponseEntity<Map<String, String>> handleTooManyAttempts(TooManyAttemptsException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header("Retry-After", "60")
			.body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getField() + " " + error.getDefaultMessage())
			.orElse("Invalid request");
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}
}
