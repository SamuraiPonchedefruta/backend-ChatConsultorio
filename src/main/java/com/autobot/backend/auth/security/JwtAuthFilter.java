package com.autobot.backend.auth.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.autobot.backend.auth.service.JwtService;
import com.autobot.backend.auth.service.JwtService.DatosToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final Predicate<String> tokenRevocadoChecker;

    public JwtAuthFilter(JwtService jwtService, Predicate<String> tokenRevocadoChecker) {
        this.jwtService = jwtService;
        this.tokenRevocadoChecker = tokenRevocadoChecker;
    }

    public JwtAuthFilter(JwtService jwtService) {
        this(jwtService, jti -> false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            jwtService.validar(header.substring(7).trim()).ifPresent(datos -> {
                if (datos.jti() != null && tokenRevocadoChecker.test(datos.jti())) {
                    log.debug("Token revocado en {}", request.getRequestURI());
                    return;
                }
                var authorities = datos.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(datos.correo(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        filterChain.doFilter(request, response);
    }
}