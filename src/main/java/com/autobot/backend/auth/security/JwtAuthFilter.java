package com.autobot.backend.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.autobot.backend.auth.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee "Authorization: Bearer <token>" y, si el token es válido, deja autenticado al usuario
 * (nombre = correo, autoridades = ROLE_ + cada rol del token).
 *
 * El filtro nunca responde por su cuenta: si no hay token o es inválido (vencido, alterado...) deja la
 * petición sin autenticar y es Spring Security quien responde 401 en las rutas protegidas.
 * Las rutas públicas siguen abiertas aunque llegue un token malo.
 *
 * No es un bean a propósito: lo crea SecurityConfig y solo corre dentro de la cadena de seguridad.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            jwtService.validar(token).ifPresent(datos -> {
                List<GrantedAuthority> autoridades = datos.roles().stream()
                        .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();
                UsernamePasswordAuthenticationToken autenticacion =
                        UsernamePasswordAuthenticationToken.authenticated(datos.correo(), null, autoridades);
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(autenticacion);
                SecurityContextHolder.setContext(contexto);
            });
        }

        cadena.doFilter(request, response);
    }
}
