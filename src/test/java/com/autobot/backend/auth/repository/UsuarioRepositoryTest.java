package com.autobot.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
        // Arrange (usamos un sufijo único para evitar colisiones en la BD local)
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String correo = "juan.perez_" + suffix + "@autobot.com";
        String telefono = "+5076" + String.format("%07d", Math.abs(suffix.hashCode() % 10000000));

        Usuario usuario = new Usuario("Juan Pérez", correo, "hash_seguro_123");
        usuario.agregarTelefono(telefono, true);
        
        usuarioRepository.save(usuario);

        // Act & Assert por Correo
        Optional<Usuario> encontradoPorCorreo = usuarioRepository.findByCorreo(correo);
        assertThat(encontradoPorCorreo).isPresent();
        assertThat(encontradoPorCorreo.get().getNombre()).isEqualTo("Juan Pérez");
        assertThat(encontradoPorCorreo.get().getFechaRegistro()).isNotNull();
        assertThat(encontradoPorCorreo.get().getFechaActualizacion()).isNotNull();
        assertThat(encontradoPorCorreo.get().isActivo()).isTrue();

        // Act & Assert por Número de Teléfono
        Optional<Usuario> encontradoPorTelefono = usuarioRepository.findByTelefonoNumero(telefono);
        assertThat(encontradoPorTelefono).isPresent();
        assertThat(encontradoPorTelefono.get().getCorreo()).isEqualTo(correo);
        assertThat(encontradoPorTelefono.get().getTelefonos()).hasSize(1);
        assertThat(encontradoPorTelefono.get().getTelefonos().get(0).getNumero()).isEqualTo(telefono);
    }
}
