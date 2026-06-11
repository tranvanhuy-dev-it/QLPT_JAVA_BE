package com.qlpt.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được trống")
    private String password;

    private String email;
    private String phone;
    private String fullName;

    private String identityCard;
    private java.time.LocalDate idCardIssueDate;
    private String idCardIssuePlace;

    // SECURITY: role KHÔNG nhận từ client.
    // Endpoint /api/auth/register luôn tạo tài khoản LANDLORD.
    // Role được gán cứng trong AuthServiceImpl.registerUser()
}
