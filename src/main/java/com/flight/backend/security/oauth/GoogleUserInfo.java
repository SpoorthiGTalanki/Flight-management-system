package com.flight.backend.security.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {

    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
}
