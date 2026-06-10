package com.qlpt.backend.dto.boardinghouse;

import jakarta.validation.constraints.NotBlank;

public record BoardingHouseCameraCreateRequest(
    @NotBlank(message = "Tên camera không được để trống")
    String name,

    @NotBlank(message = "Đường dẫn luồng (stream URL) không được để trống")
    String streamUrl,

    String username,
    String password
) {}
