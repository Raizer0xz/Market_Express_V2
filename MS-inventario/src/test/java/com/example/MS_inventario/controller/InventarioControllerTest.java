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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InventarioService service;

    private Inventario inventarioEjemplo() {
        return Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L)
                .cantidad(50).stockMinimo(5).build();
    }

    // GET /sucursal/{sucursalId} → 200
    @Test
    void deberiaRetornarStockPorSucursal() throws Exception {
        when(service.verStockPorSucursal(2L)).thenReturn(List.of(inventarioEjemplo()));

        mockMvc.perform(get("/api/v1/inventario/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inventarioList[0].cantidad").value(50))
                .andExpect(jsonPath("$._embedded.inventarioList[0]._links.self.href").exists());
    }

    // GET /sucursal/{sucursalId} → 204
    @Test
    void deberiaRetornar204CuandoSucursalSinInventario() throws Exception {
        when(service.verStockPorSucursal(99L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/inventario/sucursal/99")).andExpect(status().isNoContent());
    }

    // GET /producto/{productoId}/sucursal/{sucursalId} → 200
    @Test
    void deberiaRetornarStockExacto() throws Exception {
        when(service.verStock(10L, 2L)).thenReturn(inventarioEjemplo());

        mockMvc.perform(get("/api/v1/inventario/producto/10/sucursal/2").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(50))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // GET /producto/{productoId}/sucursal/{sucursalId} → 404
    @Test
    void deberiaRetornar404CuandoNoHayStockExacto() throws Exception {
        when(service.verStock(99L, 99L))
                .thenThrow(new RuntimeException("No hay inventario para producto 99 en sucursal 99"));

        mockMvc.perform(get("/api/v1/inventario/producto/99/sucursal/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // GET /alertas → 200
    @Test
    void deberiaRetornarAlertasGlobales() throws Exception {
        when(service.obtenerAlertasGlobales()).thenReturn(List.of(inventarioEjemplo()));

        mockMvc.perform(get("/api/v1/inventario/alertas").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // POST /aumentar → 200 ADMIN
    @Test
    void deberiaAumentarStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(70).stockMinimo(5).build();
        when(service.aumentarStock(10L, 2L, 20, "Recepcion proveedor", 1L, "ADMIN"))
                .thenReturn(actualizado);

        String json = """
                {"productoId": 10, "sucursalId": 2, "cantidad": 20, "motivo": "Recepcion proveedor"}
                """;

        mockMvc.perform(post("/api/v1/inventario/aumentar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(70));

        verify(service).aumentarStock(10L, 2L, 20, "Recepcion proveedor", 1L, "ADMIN");
    }

    // POST /aumentar → 403
    @Test
    void deberiaRetornar403AlAumentarSinRolAdmin() throws Exception {
        when(service.aumentarStock(any(), any(), any(), any(), any(), eq("CLIENTE")))
                .thenThrow(new SecurityException("Acceso denegado. Se requiere rol ADMIN"));

        String json = """
                {"productoId": 10, "sucursalId": 2, "cantidad": 20, "motivo": "test"}
                """;

        mockMvc.perform(post("/api/v1/inventario/aumentar")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado. Se requiere rol ADMIN"));
    }

    // POST /reducir → 200 ADMIN
    @Test
    void deberiaReducirStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(40).stockMinimo(5).build();
        when(service.reducirStock(10L, 2L, 10, "Pedido #42", 1L, "ADMIN"))
                .thenReturn(actualizado);

        String json = """
                {"productoId": 10, "sucursalId": 2, "cantidad": 10, "motivo": "Pedido #42"}
                """;

        mockMvc.perform(post("/api/v1/inventario/reducir")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(40));
    }

    // POST /reducir → 403
    @Test
    void deberiaRetornar403AlReducirSinRolAdmin() throws Exception {
        when(service.reducirStock(any(), any(), any(), any(), any(), eq("VENDEDOR")))
                .thenThrow(new SecurityException("Acceso denegado. Se requiere rol ADMIN"));

        String json = """
                {"productoId": 10, "sucursalId": 2, "cantidad": 10, "motivo": "test"}
                """;

        mockMvc.perform(post("/api/v1/inventario/reducir")
                        .header("X-Usuario-Rol", "VENDEDOR")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden());
    }

    // POST /ajustar → 200 ADMIN
    @Test
    void deberiaAjustarStockCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(35).stockMinimo(5).build();
        when(service.ajustarStock(10L, 2L, 35, "Conteo fisico", 1L, "ADMIN"))
                .thenReturn(actualizado);

        String json = """
                {"productoId": 10, "sucursalId": 2, "nuevaCantidad": 35, "motivo": "Conteo fisico"}
                """;

        mockMvc.perform(post("/api/v1/inventario/ajustar")
                        .header("X-Usuario-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(35));
    }

    // PUT /stock-minimo → 200 ADMIN
    @Test
    void deberiaActualizarStockMinimoCuandoEsAdmin() throws Exception {
        Inventario actualizado = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L).cantidad(50).stockMinimo(10).build();
        when(service.actualizarStockMinimo(10L, 2L, 10, "ADMIN")).thenReturn(actualizado);

        mockMvc.perform(put("/api/v1/inventario/stock-minimo")
                        .header("X-Usuario-Rol", "ADMIN")
                        .param("productoId", "10")
                        .param("sucursalId", "2")
                        .param("stockMinimo", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(10));

        verify(service).actualizarStockMinimo(10L, 2L, 10, "ADMIN");
    }

    // PUT /stock-minimo → 403
    @Test
    void deberiaRetornar403AlActualizarStockMinimoSinRolAdmin() throws Exception {
        when(service.actualizarStockMinimo(any(), any(), any(), eq("CLIENTE")))
                .thenThrow(new SecurityException("Acceso denegado. Se requiere rol ADMIN"));

        mockMvc.perform(put("/api/v1/inventario/stock-minimo")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .param("productoId", "10")
                        .param("sucursalId", "2")
                        .param("stockMinimo", "10"))
                .andExpect(status().isForbidden());
    }

    // GET /historial/sucursal/{sucursalId} → 200 ADMIN
    @Test
    void deberiaRetornarHistorialDeSucursalCuandoEsAdmin() throws Exception {
        MovimientoInventario mov = MovimientoInventario.builder()
                .id(1L).productoId(10L).sucursalId(2L)
                .tipo(TipoMovimiento.ENTRADA).cantidad(20).stockResultante(70)
                .motivo("Recepcion proveedor").usuarioId(1L).build();
        when(service.historialPorSucursal(2L, "ADMIN")).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"));

        verify(service).historialPorSucursal(2L, "ADMIN");
    }

    // GET /historial/sucursal/{sucursalId} → 403
    @Test
    void deberiaRetornar403AlVerHistorialSucursalSinRolAdmin() throws Exception {
        when(service.historialPorSucursal(2L, "CLIENTE"))
                .thenThrow(new SecurityException("Acceso denegado. Se requiere rol ADMIN"));

        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    // GET /historial/producto/{productoId}/sucursal/{sucursalId} → 200
    @Test
    void deberiaRetornarHistorialDeProductoYSucursalCuandoEsAdmin() throws Exception {
        when(service.historialPorProductoYSucursal(10L, 2L, "ADMIN"))
                .thenReturn(List.of(MovimientoInventario.builder()
                        .tipo(TipoMovimiento.SALIDA).cantidad(5).build()));

        mockMvc.perform(get("/api/v1/inventario/historial/producto/10/sucursal/2")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("SALIDA"));
    }

    // GET /historial/sucursal/{sucursalId}/tipo/{tipo} → 200
    @Test
    void deberiaRetornarHistorialPorTipoCuandoEsAdmin() throws Exception {
        when(service.historialPorTipo(2L, TipoMovimiento.AJUSTE, "ADMIN"))
                .thenReturn(List.of(MovimientoInventario.builder()
                        .tipo(TipoMovimiento.AJUSTE).cantidad(15).build()));

        mockMvc.perform(get("/api/v1/inventario/historial/sucursal/2/tipo/AJUSTE")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("AJUSTE"));
    }
}