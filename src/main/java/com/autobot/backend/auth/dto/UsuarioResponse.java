package com.autobot.backend.auth.dto;

import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.model.Rol;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Set<String> roles,
        boolean activo,
        Instant fechaRegistro
) {
    public static UsuarioResponse de(Usuario usuario) {
        Set<String> nombresRoles = usuario.getRoles().stream()
                .map(Rol::getNombreRol)
                .collect(Collectors.toSet());
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                nombresRoles,
                usuario.isActivo(),
                usuario.getFechaRegistro()
        );
    }
}
