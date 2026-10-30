package com.autobot.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CambioRolRequest(
        @NotBlank String rol
) {}
