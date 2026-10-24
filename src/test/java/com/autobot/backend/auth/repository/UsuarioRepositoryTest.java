package com.autobot.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.autobot.backend.auth.model.Usuario;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void debeGuardarYBuscarUsuarioPorCorreoYTelefono() {
        // Arrange
        Usuario usuario = new Usuario("Juan Pérez", "juan.perez@autobot.com", "hash_seguro_123");
        usuario.agregarTelefono("+50761234567", true);
        
        usuarioRepository.save(usuario);

        // Act & Assert por Correo
        Optional<Usuario> encontradoPorCorreo = usuarioRepository.findByCorreo("juan.perez@autobot.com");
        assertThat(encontradoPorCorreo).isPresent();
        assertThat(encontradoPorCorreo.get().getNombre()).isEqualTo("Juan Pérez");
        assertThat(encontradoPorCorreo.get().getFechaRegistro()).isNotNull();
        assertThat(encontradoPorCorreo.get().getFechaActualizacion()).isNotNull();
        assertThat(encontradoPorCorreo.get().isActivo()).isTrue();

        // Act & Assert por Número de Teléfono
        Optional<Usuario> encontradoPorTelefono = usuarioRepository.findByTelefonoNumero("+50761234567");
        assertThat(encontradoPorTelefono).isPresent();
        assertThat(encontradoPorTelefono.get().getCorreo()).isEqualTo("juan.perez@autobot.com");
        assertThat(encontradoPorTelefono.get().getTelefonos()).hasSize(1);
        assertThat(encontradoPorTelefono.get().getTelefonos().get(0).getNumero()).isEqualTo("+50761234567");
    }
}
