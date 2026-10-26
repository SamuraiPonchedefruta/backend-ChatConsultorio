package com.autobot.backend.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.autobot.backend.auth.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El correo debe llegar ya en minúsculas (los servicios lo normalizan).
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByCorreo(String correo);

    // "Telefonos_Numero": entra a la lista de teléfonos y compara el número.
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByTelefonos_Numero(String numero);

    boolean existsByCorreo(String correo);

    boolean existsByTelefonos_Numero(String numero);
}
