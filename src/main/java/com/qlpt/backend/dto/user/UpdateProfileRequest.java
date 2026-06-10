package com.qlpt.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
    @NotBlank(message = "Họ và tên không được để trống")
    String fullName,

    @Email(message = "Email không hợp lệ")
    String email,

    String phone
) {}
