package com.qlpt.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank(message = "Token xác thực Google không được để trống")
    String credential
) {}
