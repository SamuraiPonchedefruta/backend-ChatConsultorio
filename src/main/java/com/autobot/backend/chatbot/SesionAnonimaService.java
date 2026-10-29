package com.autobot.backend.chatbot;

import com.autobot.backend.auth.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SesionAnonimaService {

    // Simulación de almacenamiento de trabajo temporal por identificador anónimo
    private final Map<String, String> trabajoAnonimoStore = new HashMap<>();

    public void guardarTrabajoTemporal(String identificadorAnonimo, String datos) {
        trabajoAnonimoStore.put(identificadorAnonimo, datos);
    }

    public Optional<String> obtenerTrabajoTemporal(String identificadorAnonimo) {
        return Optional.ofNullable(trabajoAnonimoStore.get(identificadorAnonimo));
    }

    public void migrarSesionAUsuario(String identificadorAnonimo, Usuario usuario) {
        if (identificadorAnonimo != null && trabajoAnonimoStore.containsKey(identificadorAnonimo)) {
            String datos = trabajoAnonimoStore.get(identificadorAnonimo);
            // Aquí asociarías formalmente los datos al perfil del usuario en base de datos
            // Por ejemplo, actualizando registros huérfanos con el ID del nuevo usuario.
            
            // Limpiamos el almacenamiento temporal de la sesión anónima tras migrar
            trabajoAnonimoStore.remove(identificadorAnonimo);
        }
    }
}
