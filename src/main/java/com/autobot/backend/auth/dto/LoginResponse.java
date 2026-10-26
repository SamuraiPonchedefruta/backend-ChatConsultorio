package com.autobot.backend.auth.dto;

import java.time.Instant;

public record LoginResponse(
    String token,
    String tipo,
    Instant expiraEn
) {
    public LoginResponse(String token, Instant expiraEn) {
        this(token, "Bearer", expiraEn);
    }
}
