package com.autobot.backend.auth.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(">>> FILTRE ENTER: URI=" + request.getRequestURI() + ", AuthHeader=" + request.getHeader(HttpHeaders.AUTHORIZATION));

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.toLowerCase().startsWith("bearer ")) {
            String token = header.substring(7).trim();
            System.out.println(">>> FILTRE: Token extraído con éxito, llamando a validar...");
            Optional<DatosToken> datosOpt = jwtService.validar(token);

            if (datosOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                DatosToken datos = datosOpt.get();
                System.out.println(">>> FILTRE: Token válido para correo: " + datos.correo());

                if (datos.jti() != null && tokenRevocadoChecker != null && tokenRevocadoChecker.test(datos.jti())) {
                    System.out.println(">>> FILTRE: Token revocado (jti=" + datos.jti() + ")");
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = datos.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        datos.correo(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                System.out.println(">>> FILTRE: datosOpt está vacío o ya existe autenticación");
            }
        } else {
            System.out.println(">>> FILTRE: No hay encabezado Bearer válido");
        }

        filterChain.doFilter(request, response);
    }
}