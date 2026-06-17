package com.example.MS_usuarios.controller;

import com.example.MS_usuarios.model.Usuario;
import com.example.MS_usuarios.service.ServiceUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)   // desactiva los filtros JWT/seguridad para el test
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceUsuario serviceUsuario;

    // -------------------------------------------------------------------------
    // Helper: construye un usuario de prueba
    // -------------------------------------------------------------------------
    private Usuario usuarioEjemplo() {
        return Usuario.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .email("juan@mail.com")
                .passwordHash("hashed123")
                .telefono("912345678")
                .rol("CLIENTE")
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios → 200 con lista + HATEOAS links
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarTodosLosUsuarios() throws Exception {
        when(serviceUsuario.findAll()).thenReturn(List.of(usuarioEjemplo()));

        // El controller devuelve CollectionModel (HAL JSON):
        // los ítems van dentro de "_embedded.usuarioList"
        mockMvc.perform(get("/api/v1/usuarios")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.usuarioList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.usuarioList[0].nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$._embedded.usuarioList[0].email").value("juan@mail.com"))
                .andExpect(jsonPath("$._embedded.usuarioList[0].rol").value("CLIENTE"))
                // HATEOAS links en cada ítem
                .andExpect(jsonPath("$._embedded.usuarioList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.usuarioList[0]._links.todos.href").exists())
                .andExpect(jsonPath("$._embedded.usuarioList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.usuarioList[0]._links.delete.href").exists())
                // link self de la colección
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(serviceUsuario).findAll();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios → 204 cuando no hay usuarios
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoNoHayUsuarios() throws Exception {
        when(serviceUsuario.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isNoContent());

        verify(serviceUsuario).findAll();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/{id} → 200 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarUsuarioPorId() throws Exception {
        when(serviceUsuario.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));

        mockMvc.perform(get("/api/v1/usuarios/1")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(serviceUsuario).findById(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/{id} → 404 si no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoUsuarioNoExiste() throws Exception {
        when(serviceUsuario.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado con id: 99"));

        verify(serviceUsuario).findById(99L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/usuarios → 201 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaCrearUsuario() throws Exception {
        when(serviceUsuario.existsByEmail("juan@mail.com")).thenReturn(false);
        when(serviceUsuario.save(any(Usuario.class))).thenReturn(usuarioEjemplo());

        String json = """
                {
                    "nombre": "Juan Pérez",
                    "email": "juan@mail.com",
                    "passwordHash": "hashed123",
                    "telefono": "912345678",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                // HATEOAS links presentes en la respuesta de creación
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(serviceUsuario).existsByEmail("juan@mail.com");
        verify(serviceUsuario).save(any(Usuario.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/usuarios → 409 si el email ya existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar409CuandoEmailYaExiste() throws Exception {
        when(serviceUsuario.existsByEmail("juan@mail.com")).thenReturn(true);

        String json = """
                {
                    "nombre": "Juan Pérez",
                    "email": "juan@mail.com",
                    "passwordHash": "hashed123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un usuario con ese email"));

        verify(serviceUsuario).existsByEmail("juan@mail.com");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/usuarios → 400 si faltan campos obligatorios
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoFaltanCamposObligatorios() throws Exception {
        String json = """
                {
                    "nombre": "Juan"
                }
                """;

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/usuarios/{id} → 200 con datos actualizados + links
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarUsuario() throws Exception {
        Usuario usuarioActualizado = Usuario.builder()
                .id(1L)
                .nombre("Juan Actualizado")
                .email("juan.nuevo@mail.com")
                .passwordHash("hashed123")
                .telefono("999999999")
                .rol("ADMIN")
                .build();

        when(serviceUsuario.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));
        when(serviceUsuario.save(any(Usuario.class))).thenReturn(usuarioActualizado);

        String json = """
                {
                    "nombre": "Juan Actualizado",
                    "email": "juan.nuevo@mail.com",
                    "passwordHash": "hashed123",
                    "telefono": "999999999",
                    "rol": "ADMIN"
                }
                """;

        mockMvc.perform(put("/api/v1/usuarios/1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Actualizado"))
                .andExpect(jsonPath("$.email").value("juan.nuevo@mail.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(serviceUsuario).findById(1L);
        verify(serviceUsuario).save(any(Usuario.class));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/usuarios/{id} → 404 si no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlActualizarUsuarioInexistente() throws Exception {
        when(serviceUsuario.findById(99L)).thenReturn(Optional.empty());

        String json = """
                {
                    "nombre": "Nadie",
                    "email": "nadie@mail.com",
                    "passwordHash": "hashed",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(put("/api/v1/usuarios/99")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado con id: 99"));

        verify(serviceUsuario).findById(99L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/usuarios/{id} → 200 eliminado correctamente
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarUsuario() throws Exception {
        when(serviceUsuario.deleteById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario eliminado correctamente"));

        verify(serviceUsuario).deleteById(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/usuarios/{id} → 404 si no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlEliminarUsuarioInexistente() throws Exception {
        when(serviceUsuario.deleteById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado con id: 99"));

        verify(serviceUsuario).deleteById(99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/usuarios/health → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHealthOk() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-usuarios"))
                .andExpect(jsonPath("$.estado").value("activo"));
    }
}