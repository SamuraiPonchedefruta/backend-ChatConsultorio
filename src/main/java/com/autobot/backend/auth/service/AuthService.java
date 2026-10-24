package com.autobot.backend.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.dto.RegistroRequest;
import com.autobot.backend.auth.dto.RegistroResponse;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@Service
public class AuthService {

    static final String ROL_CLIENTE = "CLIENTE";
    private static final String PREFIJO_PANAMA = "+507";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra una cuenta de cliente.
     *
     * @throws CuentaDuplicadaException si el correo o el celular ya existen
     */
    @Transactional
    public RegistroResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        String celular = normalizarCelular(solicitud.celular());

        List<String> duplicados = new ArrayList<>();
        if (usuarios.findByCorreo(correo).isPresent()) {
            duplicados.add("correo");
        }
        if (usuarios.findByTelefonoNumero(celular).isPresent()) {
            duplicados.add("celular");
        }
        if (!duplicados.isEmpty()) {
            throw CuentaDuplicadaException.de(duplicados);
        }

        Rol rolCliente = roles.findByNombreRol(ROL_CLIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + ROL_CLIENTE + " no existe en la base de datos"));

        Usuario usuario = new Usuario(
                solicitud.nombre().trim(),
                correo,
                passwordEncoder.encode(solicitud.password()));
        usuario.agregarTelefono(celular, true);
        usuario.agregarRol(rolCliente);

        Usuario guardado;
        try {
            guardado = usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            // Dos registros simultáneos pasaron la revisión previa: la base de datos tuvo la última palabra.
            throw CuentaDuplicadaException.porCarrera(ex);
        }

        return new RegistroResponse(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getCorreo(),
                celular,
                ROL_CLIENTE,
                guardado.getFechaRegistro());
    }

    /** 8 dígitos se interpretan como celular de Panamá; si ya trae "+", se respeta. */
    private static String normalizarCelular(String celular) {
        String limpio = celular.trim();
        return limpio.startsWith("+") ? limpio : PREFIJO_PANAMA + limpio;
    }
}
