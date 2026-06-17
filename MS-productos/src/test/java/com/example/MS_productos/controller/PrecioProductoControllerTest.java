package com.example.MS_productos.controller;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.model.PrecioProducto;
import com.example.MS_productos.model.Producto;
import com.example.MS_productos.service.PrecioProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrecioProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PrecioProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrecioProductoService service;

    private Producto productoEjemplo() {
        return Producto.builder()
                .id(1L)
                .nombre("Leche Entera")
                .categoria(Categoria.builder().id(1L).nombre("Lácteos").build())
                .activo(true)
                .build();
    }

    private PrecioProducto precioEjemplo() {
        return PrecioProducto.builder()
                .id(1L)
                .producto(productoEjemplo())
                .sucursalId(2L)
                .precio(new BigDecimal("1500.00"))
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/precios/producto/{productoId} → 200 con lista + HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarPreciosPorProducto() throws Exception {
        when(service.listarPorProducto(1L)).thenReturn(List.of(precioEjemplo()));

        mockMvc.perform(get("/api/v2/precios/producto/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.precioProductoList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.precioProductoList[0].precio").value(1500.00))
                .andExpect(jsonPath("$._embedded.precioProductoList[0].sucursalId").value(2))
                .andExpect(jsonPath("$._embedded.precioProductoList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.precioProductoList[0]._links.delete.href").exists())
                .andExpect(jsonPath("$._embedded.precioProductoList[0]._links.producto.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).listarPorProducto(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/precios/producto/{productoId} → 404 producto no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoProductoNoExiste() throws Exception {
        when(service.listarPorProducto(99L))
                .thenThrow(new RuntimeException("Producto no encontrado: 99"));

        mockMvc.perform(get("/api/v2/precios/producto/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Producto no encontrado: 99"));

        verify(service).listarPorProducto(99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/precios/producto/{productoId}/sucursal/{sucursalId} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarPreciosPorProductoYSucursal() throws Exception {
        when(service.listarPorProductoYSucursal(1L, 2L)).thenReturn(List.of(precioEjemplo()));

        mockMvc.perform(get("/api/v2/precios/producto/1/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.precioProductoList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.precioProductoList[0].sucursalId").value(2))
                .andExpect(jsonPath("$._embedded.precioProductoList[0].precio").value(1500.00))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos-los-precios.href").exists());

        verify(service).listarPorProductoYSucursal(1L, 2L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/precios → 201 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaCrearPrecio() throws Exception {
        when(service.crearPrecio(any(PrecioProducto.class))).thenReturn(precioEjemplo());

        String json = """
                {
                    "producto": { "id": 1 },
                    "sucursalId": 2,
                    "precio": 1500.00
                }
                """;

        mockMvc.perform(post("/api/v2/precios")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.precio").value(1500.00))
                .andExpect(jsonPath("$.sucursalId").value(2))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.producto.href").exists());

        verify(service).crearPrecio(any(PrecioProducto.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/precios → 400 producto no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoProductoNoExisteAlCrearPrecio() throws Exception {
        when(service.crearPrecio(any(PrecioProducto.class)))
                .thenThrow(new RuntimeException("Producto no encontrado"));

        String json = """
                {
                    "producto": { "id": 99 },
                    "sucursalId": 2,
                    "precio": 1500.00
                }
                """;

        mockMvc.perform(post("/api/v2/precios")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Producto no encontrado"));

        verify(service).crearPrecio(any(PrecioProducto.class));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/precios/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarPrecio() throws Exception {
        doNothing().when(service).eliminarPrecio(1L);

        mockMvc.perform(delete("/api/v2/precios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Precio eliminado correctamente"));

        verify(service).eliminarPrecio(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/precios/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlEliminarPrecioInexistente() throws Exception {
        doThrow(new RuntimeException("Precio no encontrado: 99"))
                .when(service).eliminarPrecio(99L);

        mockMvc.perform(delete("/api/v2/precios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Precio no encontrado: 99"));

        verify(service).eliminarPrecio(99L);
    }
}