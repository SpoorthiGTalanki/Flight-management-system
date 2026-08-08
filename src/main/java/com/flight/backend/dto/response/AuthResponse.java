package com.flight.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    
    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private UserResponse user;

    @JsonIgnore
    private String refreshToken; // Exclusively passed internally for cookie setting, hidden from JSON response body
}