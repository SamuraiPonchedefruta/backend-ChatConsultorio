package com.autobot.backend.auth.dto;

import java.time.Instant;

public record RegistroResponse(
    Long id,
    String nombre,
    String correo,
    String celular,
    String rol,
    Instant fechaRegistro
) {}
