package com.qlpt.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private UUID id;
    private String username;
    private String role;
    private Boolean isExpired;
}
