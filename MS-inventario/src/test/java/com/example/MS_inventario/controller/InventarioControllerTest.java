package com.example.MS_inventario.controller;

import com.example.MS_inventario.model.Inventario;
import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import com.example.MS_inventario.service.InventarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventarioService service;

    private Inventario inventarioEjemplo() {
        return Inventario.builder()
                .id(1L)
                .productoId(10L)
                .sucursalId(2L)
                .cantidad(50)
                .stockMinimo(5)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarStockPorSucursal() throws Exception {
        when(service.verStockPorSucursal(2L)).thenReturn(List.of(inventarioEjemplo()));

        mockMvc.perform(get("/api/v1/inventario/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inventarioList[0].productoId").value(10))
                .andExpect(jsonPath("$._embedded.inventarioList[0].sucursalId").value(2))
                .andExpect(jsonPath("$._embedded.inventarioList[0].cantidad").value(50))
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.sucursal.href").exists())
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.producto.href").exists())
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.alertas.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).verStockPorSucursal(2L);
    }

    @Test
    void deberiaRetornar204CuandoSucursalSinInventario() throws Exception {
        when(service.verStockPorSucursal(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventario/sucursal/99"))
                .andExpect(status().isNoContent());

        verify(service).verStockPorSucursal(99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/producto/{productoId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarStockPorProducto() throws Exception {
        when(service.verStockPorProducto(10L)).thenReturn(List.of(inventarioEjemplo()));

        mockMvc.perform(get("/api/v1/inventario/producto/10").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inventarioList[0].productoId").value(10))
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).verStockPorProducto(10L);
    }

    @Test
    void deberiaRetornar204CuandoProductoSinInventario() throws Exception {
        when(service.verStockPorProducto(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventario/producto/999"))
                .andExpect(status().isNoContent());

        verify(service).verStockPorProducto(999L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/producto/{productoId}/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarStockExacto() throws Exception {
        when(service.verStock(10L, 2L)).thenReturn(inventarioEjemplo());

        mockMvc.perform(get("/api/v1/inventario/producto/10/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(10))
                .andExpect(jsonPath("$.sucursalId").value(2))
                .andExpect(jsonPath("$.cantidad").value(50))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.sucursal.href").exists())
                .andExpect(jsonPath("$._links.producto.href").exists())
                .andExpect(jsonPath("$._links.alertas.href").exists());

        verify(service).verStock(10L, 2L);
    }

    @Test
    void deberiaRetornar404CuandoNoHayStockExacto() throws Exception {
        when(service.verStock(99L, 99L))
                .thenThrow(new RuntimeException("No hay inventario para producto 99 en sucursal 99"));

        mockMvc.perform(get("/api/v1/inventario/producto/99/sucursal/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No hay inventario para producto 99 en sucursal 99"));

        verify(service).verStock(99L, 99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/alertas
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarAlertasGlobales() throws Exception {
        Inventario bajoStock = Inventario.builder()
                .id(2L).productoId(11L).sucursalId(3L).cantidad(2).stockMinimo(5).build();
        when(service.obtenerAlertasGlobales()).thenReturn(List.of(bajoStock));

        mockMvc.perform(get("/api/v1/inventario/alertas").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inventarioList[0].cantidad").value(2))
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).obtenerAlertasGlobales();
    }

    @Test
    void deberiaRetornarListaVaciaDeAlertasGlobales() throws Exception {
        when(service.obtenerAlertasGlobales()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventario/alertas").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).obtenerAlertasGlobales();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/alertas/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarAlertasPorSucursal() throws Exception {
        Inventario bajoStock = Inventario.builder()
                .id(3L).productoId(12L).sucursalId(2L).cantidad(1).stockMinimo(5).build();
        when(service.obtenerAlertasPorSucursal(2L)).thenReturn(List.of(bajoStock));

        mockMvc.perform(get("/api/v1/inventario/alertas/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inventarioList[0].sucursalId").value(2))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).obtenerAlertasPorSucursal(2L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/inventario/aumentar
    // -------------------------------------------------------------------------
    @Test
    void deberiaAumentarStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(70).stockMinimo(5).build();
        when(service.aumentarStock(10L, 2L, 20, "Recepcion proveedor", 1L)).thenReturn(actualizado);

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 20,
                    "motivo": "Recepcion proveedor"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/aumentar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(70))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).aumentarStock(10L, 2L, 20, "Recepcion proveedor", 1L);
    }

    @Test
    void deberiaRetornar403AlAumentarSinRolAdmin() throws Exception {
        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 20,
                    "motivo": "Recepcion proveedor"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/aumentar")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).aumentarStock(any(), any(), any(), any(), any());
    }

    @Test
    void deberiaRetornar400AlAumentarConDatosInvalidos() throws Exception {
        when(service.aumentarStock(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("La cantidad debe ser mayor a 0"));

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 0,
                    "motivo": "Sin motivo"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/aumentar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser mayor a 0"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/inventario/reducir
    // -------------------------------------------------------------------------
    @Test
    void deberiaReducirStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(40).stockMinimo(5).build();
        when(service.reducirStock(10L, 2L, 10, "Pedido #42", 1L)).thenReturn(actualizado);

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 10,
                    "motivo": "Pedido #42"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/reducir")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(40))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).reducirStock(10L, 2L, 10, "Pedido #42", 1L);
    }

    @Test
    void deberiaRetornar403AlReducirSinRolAdmin() throws Exception {
        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 10,
                    "motivo": "Pedido #42"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/reducir")
                        .header("X-Usuario-Rol", "VENDEDOR")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).reducirStock(any(), any(), any(), any(), any());
    }

    @Test
    void deberiaRetornar400AlReducirConStockInsuficiente() throws Exception {
        when(service.reducirStock(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Stock insuficiente. Disponible: 5, solicitado: 10"));

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "cantidad": 10,
                    "motivo": "Pedido #42"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/reducir")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Stock insuficiente. Disponible: 5, solicitado: 10"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/inventario/ajustar
    // -------------------------------------------------------------------------
    @Test
    void deberiaAjustarStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(35).stockMinimo(5).build();
        when(service.ajustarStock(10L, 2L, 35, "Conteo fisico", 1L)).thenReturn(actualizado);

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "nuevaCantidad": 35,
                    "motivo": "Conteo fisico"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/ajustar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(35))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).ajustarStock(10L, 2L, 35, "Conteo fisico", 1L);
    }

    @Test
    void deberiaRetornar403AlAjustarSinRolAdmin() throws Exception {
        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "nuevaCantidad": 35,
                    "motivo": "Conteo fisico"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/ajustar")
                        .header("X-Usuario-Rol", "")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).ajustarStock(any(), any(), any(), any(), any());
    }

    @Test
    void deberiaRetornar400AlAjustarConCantidadNegativa() throws Exception {
        when(service.ajustarStock(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("La cantidad no puede ser negativa"));

        String json = """
                {
                    "productoId": 10,
                    "sucursalId": 2,
                    "nuevaCantidad": -1,
                    "motivo": "Conteo fisico"
                }
                """;

        mockMvc.perform(post("/api/v1/inventario/ajustar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad no puede ser negativa"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/inventario/stock-minimo
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarStockMinimoCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(50).stockMinimo(10).build();
        when(service.actualizarStockMinimo(10L, 2L, 10)).thenReturn(actualizado);

        mockMvc.perform(put("/api/v1/inventario/stock-minimo")
                        .header("X-Usuario-Rol", "ADMIN")
                        .param("productoId", "10")
                        .param("sucursalId", "2")
                        .param("stockMinimo", "10")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(10))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).actualizarStockMinimo(10L, 2L, 10);
    }

    @Test
    void deberiaRetornar403AlActualizarStockMinimoSinRolAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/inventario/stock-minimo")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .param("productoId", "10")
                        .param("sucursalId", "2")
                        .param("stockMinimo", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).actualizarStockMinimo(any(), any(), any());
    }

    @Test
    void deberiaRetornar400AlActualizarStockMinimoInexistente() throws Exception {
        when(service.actualizarStockMinimo(99L, 99L, 10))
                .thenThrow(new RuntimeException("No hay inventario para ese producto en esa sucursal"));

        mockMvc.perform(put("/api/v1/inventario/stock-minimo")
                        .header("X-Usuario-Rol", "ADMIN")
                        .param("productoId", "99")
                        .param("sucursalId", "99")
                        .param("stockMinimo", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No hay inventario para ese producto en esa sucursal"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/historial/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHistorialDeSucursalCuandoEsAdmin() throws Exception {
        MovimientoInventario mov = MovimientoInventario.builder()
                .id(1L).productoId(10L).sucursalId(2L)
                .tipo(TipoMovimiento.ENTRADA).cantidad(20).stockResultante(70)
                .motivo("Recepcion proveedor").usuarioId(1L).build();
        when(service.historialPorSucursal(2L)).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(10))
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[0].stockResultante").value(70));

        verify(service).historialPorSucursal(2L);
    }

    @Test
    void deberiaRetornar403AlVerHistorialSucursalSinRolAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).historialPorSucursal(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/historial/producto/{productoId}/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHistorialDeProductoYSucursalCuandoEsAdmin() throws Exception {
        MovimientoInventario mov = MovimientoInventario.builder()
                .id(2L).productoId(10L).sucursalId(2L)
                .tipo(TipoMovimiento.SALIDA).cantidad(5).stockResultante(45)
                .motivo("Pedido #10").usuarioId(1L).build();
        when(service.historialPorProductoYSucursal(10L, 2L)).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/v1/inventario/historial/producto/10/sucursal/2")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("SALIDA"))
                .andExpect(jsonPath("$[0].cantidad").value(5));

        verify(service).historialPorProductoYSucursal(10L, 2L);
    }

    @Test
    void deberiaRetornar403AlVerHistorialProductoSinRolAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/inventario/historial/producto/10/sucursal/2")
                        .header("X-Usuario-Rol", "VENDEDOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).historialPorProductoYSucursal(any(), any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventario/historial/sucursal/{sucursalId}/tipo/{tipo}
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHistorialPorTipoCuandoEsAdmin() throws Exception {
        MovimientoInventario mov = MovimientoInventario.builder()
                .id(3L).productoId(10L).sucursalId(2L)
                .tipo(TipoMovimiento.AJUSTE).cantidad(15).stockResultante(35)
                .motivo("Conteo fisico").usuarioId(1L).build();
        when(service.historialPorTipo(2L, TipoMovimiento.AJUSTE)).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2/tipo/AJUSTE")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$[0].motivo").value("Conteo fisico"));

        verify(service).historialPorTipo(2L, TipoMovimiento.AJUSTE);
    }

    @Test
    void deberiaRetornar403AlVerHistorialPorTipoSinRolAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2/tipo/ENTRADA")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));

        verify(service, never()).historialPorTipo(any(), any());
    }
}