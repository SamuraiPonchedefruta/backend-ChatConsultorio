package com.autobot.backend.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModeloRequest(
        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 60, message = "La marca admite máximo 60 caracteres")
        String marca,

        @NotBlank(message = "El nombre del modelo es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @Size(max = 5000, message = "El consumo admite máximo 5000 caracteres")
        String consumo,

        @Size(max = 5000, message = "El mantenimiento admite máximo 5000 caracteres")
        String mantenimiento,

        @Size(max = 5000, message = "Las fallas comunes admiten máximo 5000 caracteres")
        String fallasComunes,

        @Size(max = 5000, message = "Los repuestos admiten máximo 5000 caracteres")
        String repuestos,

        @Size(max = 5000, message = "Qué revisar admite máximo 5000 caracteres")
        String queRevisar) {
}
