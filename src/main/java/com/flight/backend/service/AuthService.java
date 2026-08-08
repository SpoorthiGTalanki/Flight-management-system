package com.flight.backend.service;

import com.flight.backend.dto.request.GoogleLoginRequest;
import com.flight.backend.dto.request.LoginRequest;
import com.flight.backend.dto.request.RegisterRequest;
import com.flight.backend.dto.response.AuthResponse;
import com.flight.backend.dto.response.UserResponse;


public interface AuthService {

    AuthResponse register(RegisterRequest request, String deviceInfo, String ipAddress);

    AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress);

    AuthResponse googleLogin(GoogleLoginRequest request, String deviceInfo, String ipAddress);

    AuthResponse refreshAccessToken(String rawRefreshToken, String deviceInfo, String ipAddress);

    UserResponse getCurrentUser(String email);

    void logout(String rawRefreshToken);

    void logoutAllDevices(String userEmail);
}
