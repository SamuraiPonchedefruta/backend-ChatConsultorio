package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.dto.RegistroRequest;
import com.autobot.backend.auth.dto.RegistroResponse;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.TipoConsentimiento;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.TokenRevocadoRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    private static final String CORREO = "ana@example.com";
    private static final String CELULAR = "+50761234567";
    private static final String CLAVE = "ClaveSegura123";

    private static final TextoLegal TEXTO_DATOS = new TextoLegal(
            "TRATAMIENTO_DATOS", "1.0", "Texto de prueba", Instant.parse("2026-01-01T00:00:00Z"));
    private static final TextoLegal TEXTO_NOTIFICACIONES = new TextoLegal(
            "NOTIFICACIONES", "1.0", "Texto de prueba", Instant.parse("2026-01-01T00:00:00Z"));

    @Mock
    private UsuarioRepository usuarios;
    
    @Mock
    private TokenRevocadoRepository tokenRevocadoRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RolRepository roles;

    @Mock
    private ConsentimientoService consentimientos;

    @Mock
    private IntentosLoginService intentosLoginService;

    @Spy
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @InjectMocks
    private AuthService service;

    private static RegistroRequest solicitud() {
        return solicitud("61234567", new DecisionConsentimiento(true, "1.0"), null);
    }

    private static RegistroRequest solicitud(
            String celular, DecisionConsentimiento datos, DecisionConsentimiento notificaciones) {
        return new RegistroRequest("Ana Perez", CORREO, celular, CLAVE, datos, notificaciones);
    }

    private static Usuario cuentaExistente(String correo) {
        return new Usuario("Otra Persona", correo, "hash");
    }

    private void prepararRegistroValido() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());
        when(roles.findByNombreRol("CLIENTE")).thenReturn(Optional.of(new Rol("CLIENTE")));
        when(consentimientos.resolverTexto(TipoConsentimiento.DATOS, "1.0")).thenReturn(TEXTO_DATOS);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuarioGuardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Registra la cuenta con rol CLIENTE, correo normalizado y celular internacional")
    void registraCuentaConRolCliente() {
        prepararRegistroValido();

        RegistroResponse respuesta = service.registrar(new RegistroRequest(
                "  Ana Perez ", "  Ana@Example.com ", "61234567", CLAVE, new DecisionConsentimiento(true, "1.0"), null));

        Usuario guardado = usuarioGuardado();
        assertThat(guardado.getNombre()).isEqualTo("Ana Perez");
        assertThat(guardado.getCorreo()).isEqualTo(CORREO);
        assertThat(guardado.getRoles()).extracting(Rol::getNombreRol).containsExactly("CLIENTE");
        assertThat(guardado.getTelefonos()).singleElement().satisfies(telefono -> {
            assertThat(telefono.getNumero()).isEqualTo(CELULAR);
            assertThat(telefono.isPrincipal()).isTrue();
        });

        assertThat(respuesta.correo()).isEqualTo(CORREO);
        assertThat(respuesta.celular()).isEqualTo(CELULAR);
        assertThat(respuesta.rol()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("La contraseña se guarda como hash BCrypt y nunca en texto plano")
    void guardaLaContrasenaConBcrypt() {
        prepararRegistroValido();

        service.registrar(solicitud());

        String hash = usuarioGuardado().getPasswordHash();
        assertThat(hash).startsWith("$2").isNotEqualTo(CLAVE);
        assertThat(encoder.matches(CLAVE, hash)).isTrue();
    }

    @Test
    @DisplayName("Guarda el consentimiento de datos vinculado a la cuenta recién creada y no inventa el de notificaciones")
    void guardaElConsentimientoDeDatos() {
        prepararRegistroValido();

        service.registrar(solicitud());

        Usuario guardado = usuarioGuardado();
        verify(consentimientos).registrar(guardado, TEXTO_DATOS, true);
        verify(consentimientos, times(1)).registrar(any(Usuario.class), any(TextoLegal.class), anyBoolean());
    }

    @Test
    @DisplayName("Si se envía la decisión de notificaciones, se guarda tal cual (aunque sea un rechazo)")
    void guardaLaDecisionDeNotificaciones() {
        prepararRegistroValido();
        when(consentimientos.resolverTexto(TipoConsentimiento.NOTIFICACIONES, "1.0"))
                .thenReturn(TEXTO_NOTIFICACIONES);

        service.registrar(solicitud(
                "61234567", new DecisionConsentimiento(true, "1.0"), new DecisionConsentimiento(false, "1.0")));

        Usuario guardado = usuarioGuardado();
        verify(consentimientos).registrar(guardado, TEXTO_DATOS, true);
        verify(consentimientos).registrar(guardado, TEXTO_NOTIFICACIONES, false);
    }

    @Test
    @DisplayName("Sin aceptar el tratamiento de datos no se crea la cuenta")
    void rechazaSiNoAceptaLosDatos() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                solicitud("61234567", new DecisionConsentimiento(false, "1.0"), null)))
                .isInstanceOf(ConsentimientoInvalidoException.class)
                .hasMessageContaining("datos");

        verify(usuarios, never()).saveAndFlush(any());
        verify(consentimientos, never()).registrar(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Una versión inexistente del texto de datos no crea la cuenta")
    void rechazaVersionInexistente() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());
        when(roles.findByNombreRol("CLIENTE")).thenReturn(Optional.of(new Rol("CLIENTE")));
        when(consentimientos.resolverTexto(TipoConsentimiento.DATOS, "1.0"))
                .thenThrow(new ConsentimientoInvalidoException("La versión 1.0 del texto legal de datos no existe."));

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOf(ConsentimientoInvalidoException.class);

        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un correo ya registrado devuelve un error claro y no guarda nada")
    void rechazaCorreoDuplicado() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(cuentaExistente(CORREO)));
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOfSatisfying(CuentaDuplicadaException.class, ex -> {
                    assertThat(ex.getMessage()).contains("correo").doesNotContain("celular");
                    assertThat(ex.getCampos()).containsExactly("correo");
                });

        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un celular ya registrado devuelve un error claro y no guarda nada")
    void rechazaCelularDuplicado() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.of(cuentaExistente("otro@example.com")));

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOfSatisfying(CuentaDuplicadaException.class, ex -> {
                    assertThat(ex.getMessage()).contains("celular").doesNotContain("correo");
                    assertThat(ex.getCampos()).containsExactly("celular");
                });

        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Si correo y celular están repetidos, el error menciona ambos")
    void rechazaCorreoYCelularDuplicados() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(cuentaExistente(CORREO)));
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.of(cuentaExistente(CORREO)));

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOfSatisfying(CuentaDuplicadaException.class, ex -> {
                    assertThat(ex.getMessage()).contains("correo").contains("celular");
                    assertThat(ex.getCampos()).containsExactly("correo", "celular");
                });

        verify(usuarios, never()).saveAndFlush(any());
    }

    @ParameterizedTest(name = "{0} se guarda como {1}")
    @CsvSource({
            "61234567,+50761234567",
            "+50761234567,+50761234567",
            "+15551234567,+15551234567"
    })
    @DisplayName("El celular se normaliza al formato internacional")
    void normalizaElCelular(String recibido, String esperado) {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(esperado)).thenReturn(Optional.empty());
        when(roles.findByNombreRol("CLIENTE")).thenReturn(Optional.of(new Rol("CLIENTE")));
        when(consentimientos.resolverTexto(TipoConsentimiento.DATOS, "1.0")).thenReturn(TEXTO_DATOS);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroResponse respuesta = service.registrar(
                solicitud(recibido, new DecisionConsentimiento(true, "1.0"), null));

        assertThat(respuesta.celular()).isEqualTo(esperado);
        assertThat(usuarioGuardado().getTelefonos()).singleElement()
                .satisfies(telefono -> assertThat(telefono.getNumero()).isEqualTo(esperado));
    }

    @Test
    @DisplayName("Si la base rechaza el guardado por una restricción única, se informa como duplicado")
    void traduceLaViolacionDeRestriccionUnica() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());
        when(roles.findByNombreRol("CLIENTE")).thenReturn(Optional.of(new Rol("CLIENTE")));
        when(consentimientos.resolverTexto(TipoConsentimiento.DATOS, "1.0")).thenReturn(TEXTO_DATOS);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOf(CuentaDuplicadaException.class)
                .hasMessageContaining("correo o celular");

        verify(consentimientos, never()).registrar(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Si el rol CLIENTE no existe en la base, falla sin guardar la cuenta")
    void fallaSiNoExisteElRolCliente() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.findByTelefonos_Numero(CELULAR)).thenReturn(Optional.empty());
        when(roles.findByNombreRol("CLIENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(solicitud()))
                .isInstanceOf(IllegalStateException.class);

        verify(usuarios, never()).saveAndFlush(any());
    }
}