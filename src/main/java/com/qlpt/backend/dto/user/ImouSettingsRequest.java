package com.qlpt.backend.dto.user;

public record ImouSettingsRequest(
    String imouAppId,
    String imouAppSecret
) {}
