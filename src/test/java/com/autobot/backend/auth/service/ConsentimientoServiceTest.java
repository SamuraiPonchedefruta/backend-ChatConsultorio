package com.autobot.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.autobot.backend.auth.dto.ConsentimientoEstadoResponse;
import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.model.Consentimiento;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.TipoConsentimiento;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.ConsentimientoRepository;
import com.autobot.backend.auth.repository.TextoLegalRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ConsentimientoServiceTest {

    private static final String CORREO = "ana@example.com";
    private static final String TIPO_DATOS = "TRATAMIENTO_DATOS";
    private static final String TIPO_NOTIFICACIONES = "NOTIFICACIONES";

    @Mock
    private ConsentimientoRepository consentimientos;

    @Mock
    private TextoLegalRepository textos;

    @Mock
    private UsuarioRepository usuarios;

    private ConsentimientoService service;
    private Usuario usuario;
    private TextoLegal textoDatos;
    private TextoLegal textoNotificaciones;

    @BeforeEach
    void preparar() {
        service = new ConsentimientoService(consentimientos, textos, usuarios);
        usuario = new Usuario("Ana Perez", CORREO, "hash");
        ReflectionTestUtils.setField(usuario, "id", 7L);
        textoDatos = textoConId(TIPO_DATOS, 1);
        textoNotificaciones = textoConId(TIPO_NOTIFICACIONES, 2);
    }

    private static TextoLegal textoConId(String tipo, int id) {
        TextoLegal texto = new TextoLegal(tipo, "1.0", "Texto de prueba", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(texto, "id", id);
        return texto;
    }

    private void usuarioAutenticado() {
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
    }

    private ConsentimientoEstadoResponse estadoDe(List<ConsentimientoEstadoResponse> estados, String tipo) {
        return estados.stream().filter(e -> e.tipo().equals(tipo)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Sin decisiones previas ningún tipo viene marcado: ambos quedan SIN_DECISION")
    void sinDecisionesNingunTipoVieneMarcado() {
        usuarioAutenticado();

        List<ConsentimientoEstadoResponse> estados = service.estadoDe(CORREO);

        assertThat(estados).hasSize(2);
        assertThat(estados).allSatisfy(estado -> {
            assertThat(estado.estado()).isEqualTo("SIN_DECISION");
            assertThat(estado.versionAceptada()).isNull();
            assertThat(estado.fechaDecision()).isNull();
        });
    }

    @Test
    @DisplayName("Aceptar notificaciones no consulta ni modifica el consentimiento de datos")
    void aceptarNotificacionesNoTocaDatos() {
        usuarioAutenticado();
        when(textos.findByTipoAndVersion(TIPO_NOTIFICACIONES, "1.0")).thenReturn(Optional.of(textoNotificaciones));
        when(consentimientos.saveAndFlush(any(Consentimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentimientoEstadoResponse respuesta = service.actualizar(
                CORREO, TipoConsentimiento.NOTIFICACIONES, new DecisionConsentimiento(true, "1.0"));

        ArgumentCaptor<Consentimiento> captor = ArgumentCaptor.forClass(Consentimiento.class);
        verify(consentimientos).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTextoLegal().getTipo()).isEqualTo(TIPO_NOTIFICACIONES);
        assertThat(captor.getValue().isAceptado()).isTrue();
        assertThat(respuesta.tipo()).isEqualTo("notificaciones");
        assertThat(respuesta.estado()).isEqualTo("ACEPTADO");

        verify(consentimientos, never())
                .findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(anyLong(), eq(TIPO_DATOS));
    }

    @Test
    @DisplayName("Repetir la misma decisión sobre la misma versión no duplica el historial")
    void repetirLaMismaDecisionNoDuplica() {
        usuarioAutenticado();
        when(textos.findByTipoAndVersion(TIPO_NOTIFICACIONES, "1.0")).thenReturn(Optional.of(textoNotificaciones));
        when(consentimientos.findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(
                7L, TIPO_NOTIFICACIONES))
                .thenReturn(Optional.of(new Consentimiento(usuario, textoNotificaciones, true)));

        ConsentimientoEstadoResponse respuesta = service.actualizar(
                CORREO, TipoConsentimiento.NOTIFICACIONES, new DecisionConsentimiento(true, "1.0"));

        verify(consentimientos, never()).saveAndFlush(any());
        assertThat(respuesta.estado()).isEqualTo("ACEPTADO");
    }

    @Test
    @DisplayName("Cambiar de decisión agrega una fila nueva y conserva la anterior")
    void cambiarDeDecisionAgregaUnaFilaNueva() {
        usuarioAutenticado();
        when(textos.findByTipoAndVersion(TIPO_NOTIFICACIONES, "1.0")).thenReturn(Optional.of(textoNotificaciones));
        when(consentimientos.findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(
                7L, TIPO_NOTIFICACIONES))
                .thenReturn(Optional.of(new Consentimiento(usuario, textoNotificaciones, true)));
        when(consentimientos.saveAndFlush(any(Consentimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentimientoEstadoResponse respuesta = service.actualizar(
                CORREO, TipoConsentimiento.NOTIFICACIONES, new DecisionConsentimiento(false, "1.0"));

        ArgumentCaptor<Consentimiento> captor = ArgumentCaptor.forClass(Consentimiento.class);
        verify(consentimientos).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isAceptado()).isFalse();
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(respuesta.estado()).isEqualTo("RECHAZADO");
    }

    @Test
    @DisplayName("Una versión de texto que no existe se rechaza sin guardar nada")
    void versionInexistenteSeRechaza() {
        usuarioAutenticado();
        when(textos.findByTipoAndVersion(TIPO_NOTIFICACIONES, "9.9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                CORREO, TipoConsentimiento.NOTIFICACIONES, new DecisionConsentimiento(true, "9.9")))
                .isInstanceOf(ConsentimientoInvalidoException.class)
                .hasMessageContaining("9.9");

        verify(consentimientos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sin una decisión explícita (aceptado vacío) no se guarda nada")
    void exigeDecisionExplicita() {
        assertThatThrownBy(() -> service.actualizar(
                CORREO, TipoConsentimiento.DATOS, new DecisionConsentimiento(null, "1.0")))
                .isInstanceOf(ConsentimientoInvalidoException.class);

        verify(consentimientos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Registrar vincula la decisión al usuario y a la versión del texto")
    void registrarVinculaUsuarioYTexto() {
        when(consentimientos.saveAndFlush(any(Consentimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(usuario, textoDatos, true);

        ArgumentCaptor<Consentimiento> captor = ArgumentCaptor.forClass(Consentimiento.class);
        verify(consentimientos).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getTextoLegal()).isSameAs(textoDatos);
        assertThat(captor.getValue().isAceptado()).isTrue();
    }

    @Test
    @DisplayName("El estado de cada tipo es independiente: datos aceptados y notificaciones rechazadas")
    void estadoDeCadaTipoEsIndependiente() {
        usuarioAutenticado();
        when(consentimientos.findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(7L, TIPO_DATOS))
                .thenReturn(Optional.of(new Consentimiento(usuario, textoDatos, true)));
        when(consentimientos.findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(
                7L, TIPO_NOTIFICACIONES))
                .thenReturn(Optional.of(new Consentimiento(usuario, textoNotificaciones, false)));
        when(textos.findFirstByTipoOrderByVigenteDesdeDesc(TIPO_DATOS)).thenReturn(Optional.of(textoDatos));

        List<ConsentimientoEstadoResponse> estados = service.estadoDe(CORREO);

        assertThat(estadoDe(estados, "datos").estado()).isEqualTo("ACEPTADO");
        assertThat(estadoDe(estados, "datos").versionAceptada()).isEqualTo("1.0");
        assertThat(estadoDe(estados, "datos").versionVigente()).isEqualTo("1.0");
        assertThat(estadoDe(estados, "notificaciones").estado()).isEqualTo("RECHAZADO");
    }
}
