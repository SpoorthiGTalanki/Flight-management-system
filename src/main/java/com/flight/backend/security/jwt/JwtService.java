package com.flight.backend.security.jwt;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.flight.backend.constants.SecurityConstants;
import com.flight.backend.entity.User;
import com.flight.backend.exception.InvalidRefreshTokenException;
import com.flight.backend.exception.JwtAuthenticationException;
import com.flight.backend.exception.RefreshTokenExpiredException;
import com.flight.backend.exception.RefreshTokenReuseException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Set<String> invalidatedJtis = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Long> userLogoutAllTimestamps = new ConcurrentHashMap<>();

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, user.getUserId());
        claims.put(SecurityConstants.CLAIM_EMAIL, user.getEmail());
        claims.put(SecurityConstants.CLAIM_ROLE, user.getRole());
        claims.put(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS);

        return buildToken(claims, user.getEmail(), jwtProperties.getAccess().getExpiration());
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, user.getUserId());
        claims.put(SecurityConstants.CLAIM_EMAIL, user.getEmail());
        claims.put(SecurityConstants.CLAIM_ROLE, user.getRole());
        claims.put(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_REFRESH);

        return buildToken(claims, user.getEmail(), jwtProperties.getRefresh().getExpiration());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiry = new Date(nowMillis + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(now) // iat
                .expiration(expiry) // exp
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateAccessToken(String token, String expectedEmail) {
        try {
            Claims claims = extractClaims(token);
            String email = claims.getSubject();
            String tokenType = claims.get(SecurityConstants.CLAIM_TYPE, String.class);

            if (!expectedEmail.equals(email) || !SecurityConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                return false;
            }

            if (isTokenExpired(claims)) {
                return false;
            }

            Date iat = claims.getIssuedAt();
            Long logoutAllTime = userLogoutAllTimestamps.get(email);
            if (logoutAllTime != null && iat != null && iat.getTime() < logoutAllTime) {
                return false;
            }

            return true;
        } catch (JwtAuthenticationException e) {
            return false;
        }
    }

    public Claims validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing or empty.");
        }

        Claims claims;
        try {
            claims = extractClaims(token);
        } catch (JwtAuthenticationException e) {
            if (e.getCause() instanceof ExpiredJwtException) {
                throw new RefreshTokenExpiredException("Refresh token has expired. Please log in again.", e);
            }
            throw new InvalidRefreshTokenException("Invalid refresh token.", e);
        }

        String tokenType = claims.get(SecurityConstants.CLAIM_TYPE, String.class);
        if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new InvalidRefreshTokenException("Token provided is not a refresh token.");
        }

        String jti = claims.getId();
        if (jti != null && invalidatedJtis.contains(jti)) {
            String email = claims.getSubject();
            if (email != null) {
                userLogoutAllTimestamps.put(email, System.currentTimeMillis());
            }
            throw new RefreshTokenReuseException("Refresh token reuse detected. Revoking all sessions.");
        }

        Date iat = claims.getIssuedAt();
        String email = claims.getSubject();
        Long logoutAllTime = userLogoutAllTimestamps.get(email);
        if (logoutAllTime != null && iat != null && iat.getTime() < logoutAllTime) {
            throw new InvalidRefreshTokenException("Session invalidated by logout-all.");
        }

        return claims;
    }

    public void invalidateRefreshToken(String token) {
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = extractClaims(token);
                String jti = claims.getId();
                if (jti != null) {
                    if (!invalidatedJtis.add(jti)) {
                        String email = claims.getSubject();
                        if (email != null) {
                            userLogoutAllTimestamps.put(email, System.currentTimeMillis());
                        }
                    }
                }
            } catch (Exception ignored) {
                // If token cannot be parsed, no operation needed
            }
        }
    }

    public void invalidateAllUserTokens(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            userLogoutAllTimestamps.put(userEmail, System.currentTimeMillis());
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        Object userIdObj = claims.get(SecurityConstants.CLAIM_USER_ID);
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT token has expired", e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid JWT token format", e);
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Invalid JWT signature", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Unsupported JWT token", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims string is empty", e);
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
