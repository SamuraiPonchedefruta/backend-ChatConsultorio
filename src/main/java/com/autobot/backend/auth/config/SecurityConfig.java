package com.autobot.backend.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.autobot.backend.auth.security.JwtAuthFilter;
import com.autobot.backend.auth.service.JwtService;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** BCrypt con el costo por defecto de Spring Security. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, "/auth/registro").permitAll()
                        // Render usa esta ruta como health check: debe seguir siendo pública.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                // Token válido: usuario autenticado. Token ausente, vencido o alterado: sin autenticar,
                // y las rutas protegidas responden 401 por el entry point de arriba.
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
