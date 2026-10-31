package com.autobot.backend.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.model.Bitacora;
import com.autobot.backend.auth.model.Bitacora.Accion;
import com.autobot.backend.auth.repository.BitacoraRepository;

@Service
public class BitacoraService {

    private final BitacoraRepository repositorio;

    public BitacoraService(BitacoraRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Bitacora registrarAlta(String correoAdmin, String correoNuevo, String rol) {
        return registrar(Accion.ALTA, correoAdmin, correoNuevo, "Rol asignado: " + rol);
    }

    @Transactional
    public Bitacora registrarDesactivacion(String correoAdmin, String correoAfectado) {
        return registrar(Accion.DESACTIVACION, correoAdmin, correoAfectado, null);
    }

    @Transactional
    public Bitacora registrarCambioRol(String correoAdmin, String correoAfectado,
            String rolAnterior, String rolNuevo) {
        return registrar(Accion.CAMBIO_ROL, correoAdmin, correoAfectado,
                "De " + rolAnterior + " a " + rolNuevo);
    }

    private Bitacora registrar(Accion accion, String correoAdmin, String correoAfectado, String detalle) {
        return repositorio.save(new Bitacora(accion, correoAdmin, correoAfectado, detalle));
    }
}
