package com.example.MS_carrito.controller;

import com.example.MS_carrito.dto.ItemCarritoDetalleDTO;
import com.example.MS_carrito.model.Carrito;
import com.example.MS_carrito.model.ItemCarrito;
import com.example.MS_carrito.service.CarritoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarritoController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarritoService service;

    private Carrito carritoEjemplo() {
        return Carrito.builder()
                .id(1L).usuarioId(10L).sucursalId(2L).estado("ACTIVO").build();
    }

    private ItemCarrito itemEjemplo() {
        return ItemCarrito.builder()
                .id(1L).productoId(5L).cantidad(2)
                .precioUnitario(new BigDecimal("1500.00")).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos → 201
    // -------------------------------------------------------------------------
    @Test
    void deberiaCrearCarrito() throws Exception {
        when(service.crearCarrito(any())).thenReturn(carritoEjemplo());

        String json = """
                { "usuarioId": 10, "sucursalId": 2 }
                """;

        mockMvc.perform(post("/api/v2/carritos")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").value(10))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.items.href").exists())
                .andExpect(jsonPath("$._links.total.href").exists())
                .andExpect(jsonPath("$._links.confirmar.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).crearCarrito(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos → 409 ya tiene carrito activo
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar409CuandoUsuarioYaTieneCarritoActivo() throws Exception {
        when(service.crearCarrito(any()))
                .thenThrow(new RuntimeException("El usuario ya tiene un carrito activo"));

        String json = """
                { "usuarioId": 10, "sucursalId": 2 }
                """;

        mockMvc.perform(post("/api/v2/carritos")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El usuario ya tiene un carrito activo"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/activo/{usuarioId} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerCarritoActivo() throws Exception {
        when(service.obtenerCarritoActivo(10L)).thenReturn(carritoEjemplo());

        mockMvc.perform(get("/api/v2/carritos/activo/10").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").value(10))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.items.href").exists())
                .andExpect(jsonPath("$._links.total.href").exists());

        verify(service).obtenerCarritoActivo(10L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/activo/{usuarioId} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoNoHayCarritoActivo() throws Exception {
        when(service.obtenerCarritoActivo(99L))
                .thenThrow(new RuntimeException("No hay carrito activo para el usuario: 99"));

        mockMvc.perform(get("/api/v2/carritos/activo/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No hay carrito activo para el usuario: 99"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/items → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarItemsDelCarrito() throws Exception {
        when(service.listarItems(1L)).thenReturn(List.of(itemEjemplo()));

        mockMvc.perform(get("/api/v2/carritos/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productoId").value(5))
                .andExpect(jsonPath("$[0].cantidad").value(2));

        verify(service).listarItems(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/items → 204 carrito vacío
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoCarritoEstaVacio() throws Exception {
        when(service.listarItems(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/carritos/1/items"))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/items/detalle → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarItemsConDetalle() throws Exception {
        ItemCarritoDetalleDTO detalle = new ItemCarritoDetalleDTO(
                1L, 5L, "Leche Entera", "litro", 2,
                new BigDecimal("1500.00"), new BigDecimal("3000.00"));

        when(service.listarItemsConDetalle(1L)).thenReturn(List.of(detalle));

        mockMvc.perform(get("/api/v2/carritos/1/items/detalle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreProducto").value("Leche Entera"))
                .andExpect(jsonPath("$[0].cantidad").value(2))
                .andExpect(jsonPath("$[0].subtotal").value(3000.00));

        verify(service).listarItemsConDetalle(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/total → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaCalcularTotalDelCarrito() throws Exception {
        when(service.calcularTotal(1L)).thenReturn(new BigDecimal("3000.00"));

        mockMvc.perform(get("/api/v2/carritos/1/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3000.00));

        verify(service).calcularTotal(1L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos/{id}/items → 201
    // -------------------------------------------------------------------------
    @Test
    void deberiaAgregarItemAlCarrito() throws Exception {
        when(service.agregarItem(eq(1L), any())).thenReturn(itemEjemplo());

        String json = """
                {
                    "productoId": 5,
                    "cantidad": 2,
                    "precioUnitario": 1500.00
                }
                """;

        mockMvc.perform(post("/api/v2/carritos/1/items")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productoId").value(5))
                .andExpect(jsonPath("$.cantidad").value(2));

        verify(service).agregarItem(eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos/{id}/items → 404 carrito no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlAgregarItemACarritoInexistente() throws Exception {
        when(service.agregarItem(eq(99L), any()))
                .thenThrow(new RuntimeException("Carrito no encontrado: 99"));

        String json = """
                { "productoId": 5, "cantidad": 1, "precioUnitario": 1500.00 }
                """;

        mockMvc.perform(post("/api/v2/carritos/99/items")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Carrito no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/items/{itemId} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarItem() throws Exception {
        doNothing().when(service).eliminarItem(1L);

        mockMvc.perform(delete("/api/v2/carritos/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Ítem eliminado correctamente"));

        verify(service).eliminarItem(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/items/{itemId} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlEliminarItemInexistente() throws Exception {
        doThrow(new RuntimeException("Ítem no encontrado: 99"))
                .when(service).eliminarItem(99L);

        mockMvc.perform(delete("/api/v2/carritos/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ítem no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarCarrito() throws Exception {
        doNothing().when(service).eliminarCarrito(1L);

        mockMvc.perform(delete("/api/v2/carritos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Carrito eliminado correctamente"));

        verify(service).eliminarCarrito(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlEliminarCarritoInexistente() throws Exception {
        doThrow(new RuntimeException("Carrito no encontrado: 99"))
                .when(service).eliminarCarrito(99L);

        mockMvc.perform(delete("/api/v2/carritos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Carrito no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/carritos/{id}/confirmar → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaConfirmarCarrito() throws Exception {
        Carrito confirmado = Carrito.builder()
                .id(1L).usuarioId(10L).sucursalId(2L).estado("CONFIRMADO").build();

        when(service.confirmarCarrito(1L)).thenReturn(confirmado);

        mockMvc.perform(put("/api/v2/carritos/1/confirmar").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.items.href").exists());

        verify(service).confirmarCarrito(1L);
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/carritos/{id}/confirmar → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlConfirmarCarritoInexistente() throws Exception {
        when(service.confirmarCarrito(99L))
                .thenThrow(new RuntimeException("Carrito no encontrado: 99"));

        mockMvc.perform(put("/api/v2/carritos/99/confirmar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Carrito no encontrado: 99"));
    }
}