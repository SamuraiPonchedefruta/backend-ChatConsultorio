package com.autobot.backend.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.dto.RegistroRequest;
import com.autobot.backend.auth.dto.RegistroResponse;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.TipoConsentimiento;
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
    private final ConsentimientoService consentimientos;

    public AuthService(
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder passwordEncoder,
            ConsentimientoService consentimientos) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.consentimientos = consentimientos;
    }

    /**
     * Registra una cuenta de cliente y, en la misma transacción, sus decisiones de consentimiento:
     * si algo falla, no queda la cuenta sin sus consentimientos ni consentimientos sin cuenta.
     *
     * @throws CuentaDuplicadaException si el correo o el celular ya existen
     * @throws ConsentimientoInvalidoException si no se aceptan los datos o una versión de texto no existe
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

        DecisionConsentimiento datos = solicitud.consentimientoDatos();
        if (datos == null || !Boolean.TRUE.equals(datos.aceptado())) {
            throw new ConsentimientoInvalidoException(
                    "Debes aceptar el tratamiento de datos personales para crear la cuenta.");
        }

        Rol rolCliente = roles.findByNombreRol(ROL_CLIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + ROL_CLIENTE + " no existe en la base de datos"));

        TextoLegal textoDatos = consentimientos.resolverTexto(TipoConsentimiento.DATOS, datos.version());
        DecisionConsentimiento notificaciones = solicitud.consentimientoNotificaciones();
        TextoLegal textoNotificaciones = notificaciones == null
                ? null
                : consentimientos.resolverTexto(TipoConsentimiento.NOTIFICACIONES, notificaciones.version());

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

        consentimientos.registrar(guardado, textoDatos, true);
        if (textoNotificaciones != null) {
            consentimientos.registrar(guardado, textoNotificaciones, Boolean.TRUE.equals(notificaciones.aceptado()));
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
