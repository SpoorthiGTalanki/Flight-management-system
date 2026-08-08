package com.flight.backend.service.impl;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flight.backend.dto.request.GoogleLoginRequest;
import com.flight.backend.dto.request.LoginRequest;
import com.flight.backend.dto.request.RegisterRequest;
import com.flight.backend.dto.response.AuthResponse;
import com.flight.backend.dto.response.UserResponse;
import com.flight.backend.entity.User;
import com.flight.backend.exception.InvalidCredentialsException;
import com.flight.backend.exception.UserAlreadyExistsException;
import com.flight.backend.repository.UserRepository;
import com.flight.backend.security.jwt.JwtProperties;
import com.flight.backend.security.jwt.JwtService;
import com.flight.backend.security.oauth.GoogleTokenVerifier;
import com.flight.backend.security.oauth.GoogleUserInfo;
import com.flight.backend.service.AuthService;

import io.jsonwebtoken.Claims;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtProperties jwtProperties,
                           AuthenticationManager authenticationManager,
                           GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceInfo, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email '" + request.getEmail() + "' already exists.");
        }

        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("customer")
                .build();

        User savedUser = userRepository.save(newUser);
        return createAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password combination.", ex);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password combination."));

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request, String deviceInfo, String ipAddress) {
        GoogleUserInfo googleUser = googleTokenVerifier.verifyToken(request.getIdToken());

        User user = userRepository.findByEmail(googleUser.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
                    .firstName(googleUser.getFirstName())
                    .lastName(googleUser.getLastName())
                    .email(googleUser.getEmail())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role("customer")
                    .build();
            return userRepository.save(newUser);
        });

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken, String deviceInfo, String ipAddress) {
        Claims claims = jwtService.validateRefreshToken(rawRefreshToken);
        String userEmail = claims.getSubject();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found for email: " + userEmail));

        // ROTATION: Invalidate the used refresh token so it cannot be re-used
        jwtService.invalidateRefreshToken(rawRefreshToken);

        // Generate new Access Token + new Refresh Token
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            jwtService.invalidateRefreshToken(rawRefreshToken);
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            jwtService.invalidateAllUserTokens(userEmail);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new InvalidCredentialsException("User not found."));

        return UserResponse.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .build();
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken(user);

        UserResponse userResponse = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccess().getExpiration() / 1000)
                .user(userResponse)
                .refreshToken(rawRefreshToken)
                .build();
    }
}