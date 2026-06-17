package com.example.MS_productos.controller;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.service.CategoriaServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaServiceTest service;

    private Categoria categoriaEjemplo() {
        return Categoria.builder()
                .id(1L)
                .nombre("Lácteos")
                .descripcion("Productos lácteos")
                .imagenUrl("https://img.example.com/lacteos.png")
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias → 200 con lista + HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarTodasLasCategorias() throws Exception {
        when(service.listarCategorias()).thenReturn(List.of(categoriaEjemplo()));

        mockMvc.perform(get("/api/v2/categorias").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.categoriaList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.categoriaList[0].nombre").value("Lácteos"))
                .andExpect(jsonPath("$._embedded.categoriaList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.categoriaList[0]._links.todas.href").exists())
                .andExpect(jsonPath("$._embedded.categoriaList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.categoriaList[0]._links.delete.href").exists())
                .andExpect(jsonPath("$._embedded.categoriaList[0]._links.productos.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).listarCategorias();
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias → 204 cuando no hay categorías
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoNoHayCategorias() throws Exception {
        when(service.listarCategorias()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/categorias"))
                .andExpect(status().isNoContent());

        verify(service).listarCategorias();
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias/{id} → 200 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarCategoriaPorId() throws Exception {
        when(service.obtenerPorId(1L)).thenReturn(categoriaEjemplo());

        mockMvc.perform(get("/api/v2/categorias/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Lácteos"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todas.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.productos.href").exists());

        verify(service).obtenerPorId(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoCategoriaNoExiste() throws Exception {
        when(service.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Categoría no encontrada: 99"));

        mockMvc.perform(get("/api/v2/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Categoría no encontrada: 99"));

        verify(service).obtenerPorId(99L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/categorias → 201 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaCrearCategoria() throws Exception {
        when(service.crearCategoria(any(Categoria.class))).thenReturn(categoriaEjemplo());

        String json = """
                {
                    "nombre": "Lácteos",
                    "descripcion": "Productos lácteos",
                    "imagenUrl": "https://img.example.com/lacteos.png"
                }
                """;

        mockMvc.perform(post("/api/v2/categorias")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Lácteos"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todas.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).crearCategoria(any(Categoria.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/categorias → 400 datos inválidos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoFaltaNombre() throws Exception {
        String json = """
                {
                    "descripcion": "sin nombre"
                }
                """;

        mockMvc.perform(post("/api/v2/categorias")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(service, never()).crearCategoria(any());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/categorias/{id} → 200 actualizado + links
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarCategoria() throws Exception {
        Categoria actualizada = Categoria.builder()
                .id(1L)
                .nombre("Lácteos y Huevos")
                .descripcion("Lácteos y huevos frescos")
                .build();

        when(service.actualizarCategoria(eq(1L), any(Categoria.class))).thenReturn(actualizada);

        String json = """
                {
                    "nombre": "Lácteos y Huevos",
                    "descripcion": "Lácteos y huevos frescos"
                }
                """;

        mockMvc.perform(put("/api/v2/categorias/1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Lácteos y Huevos"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).actualizarCategoria(eq(1L), any(Categoria.class));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/categorias/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlActualizarCategoriaInexistente() throws Exception {
        when(service.actualizarCategoria(eq(99L), any(Categoria.class)))
                .thenThrow(new RuntimeException("Categoría no encontrada: 99"));

        String json = """
                {
                    "nombre": "X"
                }
                """;

        mockMvc.perform(put("/api/v2/categorias/99")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Categoría no encontrada: 99"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/categorias/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarCategoria() throws Exception {
        doNothing().when(service).eliminarCategoria(1L);

        mockMvc.perform(delete("/api/v2/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Categoría eliminada correctamente"));

        verify(service).eliminarCategoria(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/categorias/{id} → 400 tiene productos asociados
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400AlEliminarCategoriaConProductos() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar la categoría porque tiene productos asociados."))
                .when(service).eliminarCategoria(1L);

        mockMvc.perform(delete("/api/v2/categorias/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede eliminar la categoría porque tiene productos asociados."));

        verify(service).eliminarCategoria(1L);
    }
}