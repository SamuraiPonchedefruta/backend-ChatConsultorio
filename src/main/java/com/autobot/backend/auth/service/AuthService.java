package com.autobot.backend.auth.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String DUMMY_HASH = "$2a$10$123456789012345678901uG7W";

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
        String inputIdentificador = request.identificador().trim().toLowerCase(Locale.ROOT);

        // 1. Resolver si el input corresponde a una cuenta real (por correo o por celular)
        Optional<Usuario> usuarioOpt = usuarios.findByCorreo(inputIdentificador)
                .or(() -> usuarios.findByTelefonos_Numero(inputIdentificador));

        // 2. Unificar la clave de intentos: si el usuario existe, usamos su correo oficial. 
        // Si no existe (fantasma), usamos el input que escribió el usuario.
        String claveIntentos = usuarioOpt.isPresent() 
                ? usuarioOpt.get().getCorreo() 
                : inputIdentificador;

        // 3. Verificar si esa clave está bloqueada (lanza 429 si lo está)
        intentosLoginService.verificarNoBloqueado(claveIntentos);

        boolean passwordValida;
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            passwordValida = passwordEncoder.matches(request.password(), usuario.getPasswordHash());
        } else {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            passwordValida = false;
        }

        if (usuarioOpt.isEmpty() || !passwordValida) {
            // 4. Registrar el fallo usando la clave unificada de la cuenta
            intentosLoginService.registrarFallo(claveIntentos);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, 
                    "Credenciales inválidas"
            );
        }

        // 5. Login exitoso: limpiar los intentos de esa cuenta
        intentosLoginService.registrarExito(claveIntentos);

        Usuario usuario = usuarioOpt.get();
        List<String> rolesNombres = usuario.getRoles() != null
                ? usuario.getRoles().stream().map(Rol::getNombreRol).toList()
                : List.of(ROL_CLIENTE);

        JwtService.TokenEmitido emitido = jwtService.emitir(usuario.getCorreo(), rolesNombres);
        return new LoginResponse(emitido.token(), emitido.expiraEn());
    }
    
    public void logout(String jti, Instant expiracion) {
        if (jti != null) {
            tokenRevocadoRepository.registrar(jti, expiracion);
        }
    }

    private static String normalizarCelular(String celular) {
        String limpio = celular.trim();
        return limpio.startsWith("+") ? limpio : PREFIJO_PANAMA + limpio;
    }
}