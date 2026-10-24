package com.autobot.backend.auth.service;

import java.util.List;

/** Se lanza cuando el correo o el celular ya pertenecen a otra cuenta. */
public class CuentaDuplicadaException extends RuntimeException {

    private final List<String> campos;

    private CuentaDuplicadaException(String mensaje, List<String> campos, Throwable causa) {
        super(mensaje, causa);
        this.campos = List.copyOf(campos);
    }

    /** Duplicado detectado antes de guardar; campos puede ser "correo", "celular" o ambos. */
    public static CuentaDuplicadaException de(List<String> campos) {
        String detalle = String.join(" y ", campos.stream().map(c -> "ese " + c).toList());
        return new CuentaDuplicadaException(
                "Ya existe una cuenta registrada con " + detalle + ".", campos, null);
    }

    /** Dos registros simultáneos pasaron la revisión previa y frenó la restricción única de la base. */
    public static CuentaDuplicadaException porCarrera(Throwable causa) {
        return new CuentaDuplicadaException(
                "Ya existe una cuenta registrada con ese correo o celular.",
                List.of("correo", "celular"), causa);
    }

    public List<String> getCampos() {
        return campos;
    }
}
