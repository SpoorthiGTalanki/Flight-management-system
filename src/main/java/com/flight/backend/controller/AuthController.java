package com.flight.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.flight.backend.dto.response.UserResponse;

import com.flight.backend.constants.SecurityConstants;
import com.flight.backend.dto.request.GoogleLoginRequest;
import com.flight.backend.dto.request.LoginRequest;
import com.flight.backend.dto.request.RegisterRequest;
import com.flight.backend.dto.response.AuthResponse;
import com.flight.backend.security.cookie.CookieService;
import com.flight.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 @RequestHeader(value = SecurityConstants.HEADER_USER_AGENT, required = false) String userAgent,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {

        String ipAddress = getClientIp(httpRequest);
        AuthResponse response = authService.register(request, userAgent, ipAddress);

        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              @RequestHeader(value = SecurityConstants.HEADER_USER_AGENT, required = false) String userAgent,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {

        String ipAddress = getClientIp(httpRequest);
        AuthResponse response = authService.login(request, userAgent, ipAddress);

        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                    @RequestHeader(value = SecurityConstants.HEADER_USER_AGENT, required = false) String userAgent,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {

        String ipAddress = getClientIp(httpRequest);
        AuthResponse response = authService.googleLogin(request, userAgent, ipAddress);

        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader(value = SecurityConstants.HEADER_USER_AGENT, required = false) String userAgent,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {

        String rawRefreshToken = cookieService.extractRefreshToken(httpRequest);
        String ipAddress = getClientIp(httpRequest);

        AuthResponse response = authService.refreshAccessToken(rawRefreshToken, userAgent, ipAddress);

        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefreshToken = cookieService.extractRefreshToken(httpRequest);
        authService.logout(rawRefreshToken);
        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication, HttpServletResponse httpResponse) {
        if (authentication != null && authentication.getName() != null) {
            authService.logoutAllDevices(authentication.getName());
        }
        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(
            authService.getCurrentUser(authentication.getName())
        );
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.createRefreshTokenCookie(refreshToken).toString());
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.createCleanRefreshTokenCookie().toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(SecurityConstants.HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
