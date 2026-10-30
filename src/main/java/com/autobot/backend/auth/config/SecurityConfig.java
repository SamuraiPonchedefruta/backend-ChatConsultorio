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

import com.autobot.backend.auth.repository.TokenRevocadoRepository;
import com.autobot.backend.auth.security.JwtAuthFilter;
import com.autobot.backend.auth.security.RestAccessDeniedHandler;
import com.autobot.backend.auth.service.JwtService;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * Herramientas de consulta de acceso anónimo (sin JWT): chatbot, checklist, calculadora y semáforo.
     * Cualquier ruta nueva que deba ser pública se declara aquí; todo lo demás exige autenticación.
     */
    static final String[] RUTAS_PUBLICAS = {
            "/chat/**",
            "/checklist/**",
            "/calculadora/**",
            "/semaforo/**"
    };

    /** BCrypt con el costo por defecto de Spring Security. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            JwtService jwtService, 
            TokenRevocadoRepository tokensRevocados,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(rutas -> rutas
        .requestMatchers(HttpMethod.POST, "/auth/registro").permitAll()
        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
        .requestMatchers(RUTAS_PUBLICAS).permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")   // <-- nueva
        .anyRequest().authenticated())
                // Token válido y no revocado: usuario autenticado. Token ausente, vencido, alterado o cerrado
                // con logout: sin autenticar, y las rutas protegidas responden 401 por el entry point.
                .addFilterBefore(
                            new JwtAuthFilter(jwtService, jti -> tokensRevocados.existsById(jti)),
                            UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
