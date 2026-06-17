package com.example.MS_usuarios.controller;

import com.example.MS_usuarios.model.Usuario;
import com.example.MS_usuarios.service.ServiceUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * UsuarioController - Endpoints de MS-usuarios (puerto 8084)
 *
 * POST   /api/v1/usuarios               -> crear usuario
 * GET    /api/v1/usuarios               -> listar todos
 * GET    /api/v1/usuarios/{id}          -> buscar por ID
 * GET    /api/v1/usuarios/email/{email} -> buscar por email
 * GET    /api/v1/usuarios/rol/{rol}     -> listar por rol
 * PUT    /api/v1/usuarios/{id}          -> actualizar usuario
 * DELETE /api/v1/usuarios/{id}          -> eliminar usuario
 * GET    /api/v1/usuarios/health        -> health check
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema Market Express")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final ServiceUsuario serviceUsuario;

    // -------------------------------------------------------------------------
    // POST /api/v1/usuarios
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Crear nuevo usuario",
            description = "Registra un nuevo usuario en el sistema. El email debe ser único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(schema = @Schema(implementation = Usuario.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\": \"El nombre es obligatorio\"}"))),
            @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Ya existe un usuario con ese email\"}")))
    })
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

        agregarLinks(guardado);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Listar todos los usuarios",
            description = "Retorna la lista completa de usuarios registrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios retornada exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay usuarios registrados")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Usuario>> listarTodos() {
        log.info("Listando todos los usuarios");
        List<Usuario> usuarios = serviceUsuario.findAll();

        if (usuarios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        usuarios.forEach(this::agregarLinks);

        CollectionModel<Usuario> response = CollectionModel.of(
                usuarios,
                linkTo(methodOn(UsuarioController.class).listarTodos()).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/{id}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Buscar usuario por ID",
            description = "Retorna un usuario específico. Usado internamente por MS-seguridad para validar existencia."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = Usuario.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Usuario no encontrado con id: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Buscando usuario con id: {}", id);
        return serviceUsuario.findById(id)
                .<ResponseEntity<?>>map(u -> {
                    agregarLinks(u);
                    return ResponseEntity.ok(u);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con id: " + id)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/email/{email}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Buscar usuario por email",
            description = "Retorna el usuario asociado al email indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = Usuario.class))),
            @ApiResponse(responseCode = "404", description = "No existe usuario con ese email")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<?> buscarPorEmail(
            @Parameter(description = "Email del usuario", required = true, example = "juan@mail.com")
            @PathVariable String email) {
        log.info("Buscando usuario con email: {}", email);
        return serviceUsuario.findByEmail(email)
                .<ResponseEntity<?>>map(u -> {
                    agregarLinks(u);
                    return ResponseEntity.ok(u);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con email: " + email)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/rol/{rol}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Listar usuarios por rol",
            description = "Retorna todos los usuarios que tienen el rol especificado (ej: CLIENTE, ADMIN)."
    )
    @ApiResponse(responseCode = "200", description = "Lista de usuarios por rol")
    @GetMapping("/rol/{rol}")
    public ResponseEntity<CollectionModel<Usuario>> buscarPorRol(
            @Parameter(description = "Rol del usuario", required = true, example = "CLIENTE")
            @PathVariable String rol) {
        log.info("Listando usuarios con rol: {}", rol);
        List<Usuario> usuarios = serviceUsuario.findByRol(rol);

        usuarios.forEach(this::agregarLinks);

        CollectionModel<Usuario> response = CollectionModel.of(
                usuarios,
                linkTo(methodOn(UsuarioController.class).buscarPorRol(rol)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/usuarios/{id}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza nombre, email, teléfono y rol del usuario. El passwordHash NO se modifica aquí."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado",
                    content = @Content(schema = @Schema(implementation = Usuario.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @Parameter(description = "ID del usuario a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody Usuario datosNuevos) {
        log.info("Actualizando usuario con id: {}", id);
        return serviceUsuario.findById(id)
                .<ResponseEntity<?>>map(existente -> {
                    existente.setNombre(datosNuevos.getNombre());
                    existente.setEmail(datosNuevos.getEmail());
                    existente.setTelefono(datosNuevos.getTelefono());
                    existente.setRol(datosNuevos.getRol());
                    // passwordHash NO se actualiza aquí — responsabilidad de MS-seguridad
                    Usuario actualizado = serviceUsuario.save(existente);
                    log.info("Usuario {} actualizado correctamente", id);
                    agregarLinks(actualizado);
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado con id: " + id)));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/usuarios/{id}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Eliminar usuario",
            description = "Elimina permanentemente el usuario con el ID indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario eliminado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Usuario eliminado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @Parameter(description = "ID del usuario a eliminar", required = true, example = "1")
            @PathVariable Long id) {
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

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/health
    // -------------------------------------------------------------------------
    @Operation(summary = "Health check", description = "Verifica que el microservicio esté activo.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-usuarios",
                "estado",   "activo",
                "puerto",   "8084"
        ));
    }

    // -------------------------------------------------------------------------
    // HATEOAS: método privado que agrega los links hipermedia a cada usuario
    // -------------------------------------------------------------------------
    private void agregarLinks(Usuario usuario) {
        usuario.add(
                linkTo(methodOn(UsuarioController.class).buscarPorId(usuario.getId()))
                        .withSelfRel()
        );
        usuario.add(
                linkTo(methodOn(UsuarioController.class).listarTodos())
                        .withRel("todos")
        );
        usuario.add(
                linkTo(methodOn(UsuarioController.class).actualizarUsuario(usuario.getId(), usuario))
                        .withRel("update")
        );
        usuario.add(
                linkTo(methodOn(UsuarioController.class).eliminarUsuario(usuario.getId()))
                        .withRel("delete")
        );
    }
}