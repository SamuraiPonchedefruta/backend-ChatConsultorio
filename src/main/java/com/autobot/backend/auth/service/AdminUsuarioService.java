package com.autobot.backend.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.auth.dto.CuentaPersonalRequest;
import com.autobot.backend.auth.model.Rol;
import com.autobot.backend.auth.model.Usuario;
import com.autobot.backend.auth.repository.RolRepository;
import com.autobot.backend.auth.repository.UsuarioRepository;

@Service
public class AdminUsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository rolesRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminUsuarioService(UsuarioRepository usuarios, RolRepository rolesRepo, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.rolesRepo = rolesRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario crear(CuentaPersonalRequest req) {
        String correoNormalizado = req.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Rol rol = rolesRepo.findByNombreRol(req.rol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado"));

        Usuario u = new Usuario(req.nombre(), correoNormalizado, passwordEncoder.encode(req.contrasena()));
        u.setActivo(true);
        u.agregarRol(rol);
        return usuarios.save(u);
    }

    @Transactional
    public Usuario cambiarRol(Long id, String nombreRol, String correoAdminActual) {
        Usuario u = buscar(id);
        if (u.getCorreo().equalsIgnoreCase(correoAdminActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés cambiar tu propio rol");
        }

        Rol rol = rolesRepo.findByNombreRol(nombreRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado"));

        u.getRoles().clear();
        u.agregarRol(rol);
        return u;
    }

    @Transactional
    public Usuario desactivar(Long id, String correoAdminActual) {
        Usuario u = buscar(id);
        if (u.getCorreo().equalsIgnoreCase(correoAdminActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés desactivar tu propia cuenta");
        }
        u.setActivo(false);
        return u;
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
