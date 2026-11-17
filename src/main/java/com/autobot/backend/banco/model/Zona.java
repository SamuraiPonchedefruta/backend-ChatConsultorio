package com.autobot.backend.banco.model;

/** Zona del vehículo a la que pertenecen las preguntas del banco. */
public record Zona(
        Integer idZona,
        String codigo,
        String nombre,
        int orden) {
}
