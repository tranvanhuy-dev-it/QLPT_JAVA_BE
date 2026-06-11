package com.qlpt.backend.dto.boardinghouse;

import jakarta.validation.constraints.NotBlank;

public record BoardingHouseCameraCreateRequest(
    @NotBlank(message = "Tên camera không được để trống")
    String name,

    String streamUrl,

    String username,
    String password,
    String brand,
    String serialNumber,
    String safetyCode
) {}
