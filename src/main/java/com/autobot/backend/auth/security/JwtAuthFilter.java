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

/**
 * Lee "Authorization: Bearer <token>" y, si el token es válido y no fue revocado con logout, deja
 * autenticado al usuario (nombre = correo, autoridades = ROLE_ + cada rol del token).
 *
 * Nunca responde por su cuenta: si el token falta, es inválido o está revocado, deja la petición sin
 * autenticar y Spring Security responde 401 en las rutas protegidas.
 *
 * Seguridad: este filtro no registra el encabezado Authorization ni el token, ni siquiera en nivel debug.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final Predicate<String> tokenRevocadoChecker;

    public JwtAuthFilter(JwtService jwtService, Predicate<String> tokenRevocadoChecker) {
        this.jwtService = jwtService;
        this.tokenRevocadoChecker = tokenRevocadoChecker;
    }

    /** Sin lista negra: solo para probar el filtro. */
    public JwtAuthFilter(JwtService jwtService) {
        this(jwtService, jti -> false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            String token = header.substring(PREFIJO.length()).trim();
            Optional<DatosToken> datosOpt = jwtService.validar(token);

            if (datosOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                DatosToken datos = datosOpt.get();

                if (datos.jti() != null && tokenRevocadoChecker != null && tokenRevocadoChecker.test(datos.jti())) {
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
            }
        }

        filterChain.doFilter(request, response);
    }
}
