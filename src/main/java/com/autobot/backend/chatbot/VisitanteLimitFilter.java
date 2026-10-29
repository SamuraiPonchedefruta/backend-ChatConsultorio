package com.autobot.backend.chatbot;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class VisitanteLimitFilter extends OncePerRequestFilter {

    private final VisitanteLimitService visitanteLimitService;

    public VisitanteLimitFilter(VisitanteLimitService visitanteLimitService) {
        this.visitanteLimitService = visitanteLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Aplicar solo a rutas del chatbot para visitantes no autenticados (puedes ajustar el patrón según tu arquitectura)
        String path = request.getRequestURI();
        if (path.startsWith("/api/chatbot")) {
            // Obtener identificador anónimo por IP o cabecera personalizada
            String identificador = obtenerIdentificadorAnonimo(request);

            if (!visitanteLimitService.permitirInteraccion(identificador)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"mensaje\": \"Has superado el límite diario de interacciones para visitantes anónimos. Por favor, regístrate para continuar.\"}");
                return;
            }

            visitanteLimitService.registrarInteraccion(identificador);
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerIdentificadorAnonimo(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
