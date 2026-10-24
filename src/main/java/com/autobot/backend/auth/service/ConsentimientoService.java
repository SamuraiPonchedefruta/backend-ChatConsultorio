package com.autobot.backend.auth.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobot.backend.auth.dto.ConsentimientoEstadoResponse;
import com.autobot.backend.auth.dto.DecisionConsentimiento;
import com.autobot.backend.auth.model.Consentimiento;
import com.autobot.backend.auth.model.TextoLegal;
import com.autobot.backend.auth.model.TipoConsentimiento;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.ConsentimientoRepository;
import com.autobot.backend.auth.repository.TextoLegalRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

/**
 * Gestiona las decisiones de consentimiento. Cada tipo (datos, notificaciones) tiene su propio
 * historial: decidir sobre uno nunca modifica el estado del otro.
 */
@Service
public class ConsentimientoService {

    static final String ACEPTADO = "ACEPTADO";
    static final String RECHAZADO = "RECHAZADO";
    static final String SIN_DECISION = "SIN_DECISION";

    private final ConsentimientoRepository consentimientos;
    private final TextoLegalRepository textos;
    private final UsuarioRepository usuarios;

    public ConsentimientoService(
            ConsentimientoRepository consentimientos, TextoLegalRepository textos, UsuarioRepository usuarios) {
        this.consentimientos = consentimientos;
        this.textos = textos;
        this.usuarios = usuarios;
    }

    /** Busca la versión del texto que la persona dice haber visto. */
    @Transactional(readOnly = true)
    public TextoLegal resolverTexto(TipoConsentimiento tipo, String version) {
        if (version == null || version.isBlank()) {
            throw new ConsentimientoInvalidoException("Indica la versión del texto legal de " + tipo.getRuta() + ".");
        }
        return textos.findByTipoAndVersion(tipo.getTipoTexto(), version.trim())
                .orElseThrow(() -> new ConsentimientoInvalidoException(
                        "La versión " + version + " del texto legal de " + tipo.getRuta() + " no existe."));
    }

    /** Guarda una decisión nueva vinculada al usuario y a la versión del texto. Siempre agrega una fila. */
    @Transactional
    public Consentimiento registrar(Usuario usuario, TextoLegal texto, boolean aceptado) {
        return consentimientos.saveAndFlush(new Consentimiento(usuario, texto, aceptado));
    }

    /**
     * Actualiza desde el perfil la decisión sobre un tipo de consentimiento.
     * Si la decisión y la versión son las mismas que la vigente no se agrega una fila repetida.
     *
     * @param correo correo del usuario autenticado
     */
    @Transactional
    public ConsentimientoEstadoResponse actualizar(
            String correo, TipoConsentimiento tipo, DecisionConsentimiento decision) {
        if (decision.aceptado() == null) {
            throw new ConsentimientoInvalidoException("Indica explícitamente si aceptas o rechazas.");
        }
        boolean aceptado = decision.aceptado();

        Usuario usuario = buscarUsuario(correo);
        TextoLegal texto = resolverTexto(tipo, decision.version());

        Optional<Consentimiento> ultima = ultimaDecision(usuario, tipo);
        boolean sinCambios = ultima
                .filter(c -> Objects.equals(c.getTextoLegal().getId(), texto.getId()) && c.isAceptado() == aceptado)
                .isPresent();

        Consentimiento vigente = sinCambios ? ultima.get() : registrar(usuario, texto, aceptado);
        return aEstado(tipo, Optional.of(vigente));
    }

    /** Estado de cada tipo de consentimiento. Un tipo sin decisión se informa como SIN_DECISION. */
    @Transactional(readOnly = true)
    public List<ConsentimientoEstadoResponse> estadoDe(String correo) {
        Usuario usuario = buscarUsuario(correo);
        return Arrays.stream(TipoConsentimiento.values())
                .map(tipo -> aEstado(tipo, ultimaDecision(usuario, tipo)))
                .toList();
    }

    private Optional<Consentimiento> ultimaDecision(Usuario usuario, TipoConsentimiento tipo) {
        return consentimientos.findFirstByUsuarioIdAndTextoLegalTipoOrderByFechaDecisionDescIdDesc(
                usuario.getId(), tipo.getTipoTexto());
    }

    private ConsentimientoEstadoResponse aEstado(TipoConsentimiento tipo, Optional<Consentimiento> decision) {
        String versionVigente = textos.findFirstByTipoOrderByVigenteDesdeDesc(tipo.getTipoTexto())
                .map(TextoLegal::getVersion)
                .orElse(null);
        return decision
                .map(c -> new ConsentimientoEstadoResponse(
                        tipo.getRuta(),
                        c.isAceptado() ? ACEPTADO : RECHAZADO,
                        c.getTextoLegal().getVersion(),
                        c.getFechaDecision(),
                        versionVigente))
                .orElseGet(() -> new ConsentimientoEstadoResponse(
                        tipo.getRuta(), SIN_DECISION, null, null, versionVigente));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarios.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalStateException("No existe el usuario autenticado"));
    }
}
