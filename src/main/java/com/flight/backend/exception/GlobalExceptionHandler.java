package com.flight.backend.exception;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("User Already Exists");
        problem.setType(URI.create("https://flight.com/errors/user-already-exists"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ProblemDetail handleInvalidCredentials(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://flight.com/errors/invalid-credentials"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Invalid Refresh Token");
        problem.setType(URI.create("https://flight.com/errors/invalid-refresh-token"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ProblemDetail handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Refresh Token Expired");
        problem.setType(URI.create("https://flight.com/errors/refresh-token-expired"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ProblemDetail handleRefreshTokenReuse(RefreshTokenReuseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Refresh Token Reuse Detected");
        problem.setType(URI.create("https://flight.com/errors/refresh-token-reuse"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(GoogleAuthenticationException.class)
    public ProblemDetail handleGoogleAuthentication(GoogleAuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Google Authentication Error");
        problem.setType(URI.create("https://flight.com/errors/google-authentication-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({JwtAuthenticationException.class, UnauthorizedException.class})
    public ProblemDetail handleUnauthorized(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://flight.com/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request parameters.");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://flight.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://flight.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
