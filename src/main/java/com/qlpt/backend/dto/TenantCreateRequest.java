package com.qlpt.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantCreateRequest {
    @NotBlank(message = "Tên đăng nhập không được trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được trống")
    private String password;

    private String email;
    private String phone;

    @NotBlank(message = "Họ và tên không được trống")
    private String fullName;

    private String identityCard;
    private java.time.LocalDate idCardIssueDate;
    private String idCardIssuePlace;
    private String permanentAddress;
}
