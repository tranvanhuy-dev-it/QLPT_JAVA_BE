package com.qlpt.backend.dto.auth;

import com.qlpt.backend.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Vai trò không được trống")
    private Role role;
}
