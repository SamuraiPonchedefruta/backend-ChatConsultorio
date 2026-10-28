package com.autobot.backend.auth.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.dto.LoginRequest;
import com.autobot.backend.auth.dto.LoginResponse;
import com.autobot.backend.auth.dto.RegistroRequest;
import com.autobot.backend.auth.dto.RegistroResponse;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.TipoConsentimiento;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.TokenRevocadoRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@Service
public class AuthService {

    static final String ROL_CLIENTE = "CLIENTE";
    private static final String PREFIJO_PANAMA = "+507";

    /**
     * Hash BCrypt válido (costo 10, el del codificador por defecto) de una contraseña aleatoria descartada.
     * Se verifica cuando la cuenta no existe, para que esa respuesta tarde lo mismo que la de una cuenta real
     * con mala contraseña y el tiempo no delate qué cuentas existen. Debe ser un hash válido: si no lo es,
     * Spring lo rechaza al instante y la defensa no sirve.
     */
    private static final String HASH_FICTICIO = "$2a$10$BzSAF86a2n7TBSKU4bjuUe8bCPCvM8pdryf.cAhXW8g6vIFr2raI2";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConsentimientoService consentimientos;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLoginService;

    @Autowired
    public AuthService(
            UsuarioRepository usuarios,
            RolRepository roles,
            TokenRevocadoRepository tokenRevocadoRepository,
            PasswordEncoder passwordEncoder,
            ConsentimientoService consentimientos,
            JwtService jwtService,
            IntentosLoginService intentosLoginService) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.consentimientos = consentimientos;
        this.jwtService = jwtService;
        this.intentosLoginService = intentosLoginService;
    }

    @Transactional
    public RegistroResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        String celular = normalizarCelular(solicitud.celular());

        List<String> duplicados = new ArrayList<>();
        if (usuarios.findByCorreo(correo).isPresent()) {
            duplicados.add("correo");
        }
        if (usuarios.findByTelefonos_Numero(celular).isPresent()) {
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

    public LoginResponse login(LoginRequest request) {
        String identificador = request.identificador() == null ? "" : request.identificador().trim();
        if (identificador.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador no puede estar vacío");
        }

        // Con @ es un correo; cualquier otro identificador se trata como celular, igual que en el registro.
        boolean esCorreo = identificador.contains("@");
        String identificadorNormalizado = esCorreo
                ? identificador.toLowerCase(Locale.ROOT)
                : normalizarCelular(identificador);

        Optional<Usuario> usuarioOpt = esCorreo
                ? usuarios.findByCorreo(identificadorNormalizado)
                : usuarios.findByTelefonos_Numero(identificadorNormalizado);

        // Los intentos se cuentan por cuenta (su correo) si existe; si no, por lo que se escribió.
        String claveIntentos = usuarioOpt.map(Usuario::getCorreo).orElse(identificadorNormalizado);

        // 1. Si ya está bloqueada, responde 429 sin verificar la contraseña.
        intentosLoginService.verificarNoBloqueado(claveIntentos);

        boolean passwordValida;
        if (usuarioOpt.isPresent()) {
            passwordValida = request.password() != null
                    && passwordEncoder.matches(request.password(), usuarioOpt.get().getPasswordHash());
        } else {
            if (request.password() != null) {
                passwordEncoder.matches(request.password(), HASH_FICTICIO);
            }
            passwordValida = false;
        }

        // 2. Si falla, registra el fallo (lanza 429 si este intento alcanza el límite) y responde 401.
        if (!passwordValida) {
            intentosLoginService.registrarFallo(claveIntentos);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // 3. Éxito: reinicia el contador.
        intentosLoginService.registrarExito(claveIntentos);

        Usuario usuario = usuarioOpt.get();
        List<String> rolesNombres = usuario.getRoles() != null
                ? usuario.getRoles().stream().map(Rol::getNombreRol).toList()
                : List.of(ROL_CLIENTE);

        JwtService.TokenEmitido emitido = jwtService.emitir(usuario.getCorreo(), rolesNombres);
        return new LoginResponse(emitido.token(), emitido.expiraEn());
    }

    public void logout(String jti, Instant expiracion) {
        if (jti != null && expiracion != null) {
            tokenRevocadoRepository.registrar(jti, expiracion);
        }
    }

    private static String normalizarCelular(String celular) {
        String limpio = celular.trim();
        return limpio.startsWith("+") ? limpio : PREFIJO_PANAMA + limpio;
    }
}
