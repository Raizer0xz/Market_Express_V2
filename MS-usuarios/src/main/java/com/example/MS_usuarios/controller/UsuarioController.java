package com.example.MS_usuarios.controller;

import com.example.MS_usuarios.model.Usuario;
import com.example.MS_usuarios.service.ServiceUsuario;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UsuarioController - Endpoints de MS-usuarios (puerto 9090)
 *
 * POST   /api/v1/usuarios              -> crear usuario
 * GET    /api/v1/usuarios              -> listar todos
 * GET    /api/v1/usuarios/{id}         -> buscar por ID
 * GET    /api/v1/usuarios/email/{email}-> buscar por email
 * GET    /api/v1/usuarios/rol/{rol}    -> listar por rol
 * PUT    /api/v1/usuarios/{id}         -> actualizar usuario
 * DELETE /api/v1/usuarios/{id}         -> eliminar usuario
 * GET    /api/v1/usuarios/health       -> health check
 *
 * NOTA: La / inicial en @RequestMapping es OBLIGATORIA para que el Gateway
 * pueda rutear correctamente.
 */
@RestController
@RequestMapping("/api/v1/usuarios")   // <-- FIX: slash inicial agregada
@RequiredArgsConstructor
@Slf4j

public class UsuarioController {

    private final ServiceUsuario serviceUsuario;

    // --- POST /api/v1/usuarios ---
    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody Usuario usuario) {
        log.info("Solicitud de creacion de usuario con email: {}", usuario.getEmail());
        if (serviceUsuario.existsByEmail(usuario.getEmail())) {
            log.warn("Intento de registro con email duplicado: {}", usuario.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un usuario con ese email"));
        }
        Usuario guardado = serviceUsuario.save(usuario);
        log.info("Usuario creado con id: {}", guardado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // --- GET /api/v1/usuarios ---
    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        log.info("Listando todos los usuarios");
        return ResponseEntity.ok(serviceUsuario.findAll());
    }

    // --- GET /api/v1/usuarios/{id} ---
    // Endpoint clave: MS-seguridad verifica que el usuarioId exista
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        log.info("Buscando usuario con id: {}", id);
        return serviceUsuario.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con id: " + id)));
    }

    // --- GET /api/v1/usuarios/email/{email} ---
    @GetMapping("/email/{email}")
    public ResponseEntity<?> buscarPorEmail(@PathVariable String email) {
        log.info("Buscando usuario con email: {}", email);
        return serviceUsuario.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con email: " + email)));
    }

    // --- GET /api/v1/usuarios/rol/{rol} ---
    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<Usuario>> buscarPorRol(@PathVariable String rol) {
        log.info("Listando usuarios con rol: {}", rol);
        return ResponseEntity.ok(serviceUsuario.findByRol(rol));
    }

    // --- PUT /api/v1/usuarios/{id} ---
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id,
                                               @Valid @RequestBody Usuario datosNuevos) {
        log.info("Actualizando usuario con id: {}", id);
        return serviceUsuario.findById(id)
                .<ResponseEntity<?>>map(existente -> {
                    existente.setNombre(datosNuevos.getNombre());
                    existente.setEmail(datosNuevos.getEmail());
                    existente.setTelefono(datosNuevos.getTelefono());
                    existente.setRol(datosNuevos.getRol());
                    // passwordHash NO se actualiza aqui - eso es trabajo de MS-seguridad
                    Usuario actualizado = serviceUsuario.save(existente);
                    log.info("Usuario {} actualizado correctamente", id);
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con id: " + id)));
    }

    // --- DELETE /api/v1/usuarios/{id} ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        log.info("Eliminando usuario con id: {}", id);
        boolean eliminado = serviceUsuario.deleteById(id);
        if (eliminado) {
            log.info("Usuario {} eliminado correctamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
        }
        log.warn("Intento de eliminar usuario inexistente con id: {}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado con id: " + id));
    }
    @PostMapping
    public ResponseEntity<Usuario> save(@Valid @RequestBody Usuario usuario) {
        Usuario user = serviceUsuario.save(usuario);

        user.add(linkTo(methodOn(UsuarioController.class).listarTodos()).withRel("todos"));
        user.add(linkTo(methodOn(UsuarioController.class).buscarPorId(user.getId())).withSelfRel());
        user.add(linkTo(methodOn(UsuarioController.class).actualizarUsuario(usuario.getId(), usuario)).withRel("update"));
        user.add(linkTo(methodOn(UsuarioController.class).eliminarUsuario(user.getId())).withRel("delete"));
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // --- GET /api/v1/usuarios/health ---
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-usuarios",
                "estado", "activo",
                "puerto", "9090"
        ));
    }
}