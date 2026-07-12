package com.example.MS_pagos.controller;

import com.example.MS_pagos.modelo.EstadoPago;
import com.example.MS_pagos.modelo.MetodoPago;
import com.example.MS_pagos.modelo.Pago;
import com.example.MS_pagos.modelo.PagoRequest;
import com.example.MS_pagos.service.PagoService;
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

@WebMvcTest(PagoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService pagoService;

    private Pago pagoEjemplo() {
        return Pago.builder()
                .id(1L)
                .pedidoId(1L)
                .monto(15000.00)
                .moneda("CLP")
                .metodo(MetodoPago.TARJETA_CREDITO)
                .estado(EstadoPago.PROCESANDO)
                .transaccionId("abc-123-uuid")
                .build();
    }

    // POST /api/v1/pagos/procesar → 201
    @Test
    void deberiaProcesarPagoYRetornar201() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class), eq("CLIENTE")))
                .thenReturn(pagoEjemplo());

        String json = """
                {
                    "pedidoId": 1,
                    "monto": 15000.00,
                    "moneda": "CLP",
                    "metodo": "TARJETA_CREDITO"
                }
                """;

        mockMvc.perform(post("/api/v1/pagos/procesar")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .header("X-Usuario-Rol", "CLIENTE")
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PROCESANDO"))
                .andExpect(jsonPath("$._links.pagos-del-pedido.href").exists())
                .andExpect(jsonPath("$._links.confirmar.href").exists());

        verify(pagoService).procesarPago(any(PagoRequest.class), eq("CLIENTE"));
    }

    // POST /api/v1/pagos/procesar → 403
    @Test
    void deberiaRetornar403CuandoRolNoEsClienteNiAdmin() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class), eq("REPARTIDOR")))
                .thenThrow(new SecurityException("Solo clientes o administradores pueden crear pagos"));

        String json = """
                {
                    "pedidoId": 1,
                    "monto": 15000.00,
                    "metodo": "TARJETA_CREDITO"
                }
                """;

        mockMvc.perform(post("/api/v1/pagos/procesar")
                        .contentType("application/json")
                        .header("X-Usuario-Rol", "REPARTIDOR")
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo clientes o administradores pueden crear pagos"));
    }

    // POST /api/v1/pagos/procesar → 400 método inválido
    @Test
    void deberiaRetornar400CuandoMetodoDePagoEsInvalido() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class), eq("CLIENTE")))
                .thenThrow(new IllegalArgumentException("Metodo de pago invalido: 'EFECTIVO'"));

        String json = """
                {
                    "pedidoId": 1,
                    "monto": 15000.00,
                    "metodo": "EFECTIVO"
                }
                """;

        mockMvc.perform(post("/api/v1/pagos/procesar")
                        .contentType("application/json")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Metodo de pago invalido: 'EFECTIVO'"));
    }

    // POST /api/v1/pagos/procesar → 400 campos faltantes
    @Test
    void deberiaRetornar400CuandoFaltanCamposObligatorios() throws Exception {
        String json = """
                {
                    "monto": 15000.00
                }
                """;

        mockMvc.perform(post("/api/v1/pagos/procesar")
                        .contentType("application/json")
                        .header("X-Usuario-Rol", "CLIENTE")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(pagoService, never()).procesarPago(any(), any());
    }

    // GET /api/v1/pagos/metodos → 200
    @Test
    void deberiaRetornarMetodosDePagoDisponibles() throws Exception {
        when(pagoService.obtenerMetodosDisponibles()).thenReturn(List.of(
                MetodoPago.TARJETA_CREDITO, MetodoPago.TARJETA_DEBITO,
                MetodoPago.TRANSFERENCIA_BANCARIA, MetodoPago.PAYPAL));

        mockMvc.perform(get("/api/v1/pagos/metodos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("TARJETA_CREDITO"))
                .andExpect(jsonPath("$[1]").value("TARJETA_DEBITO"));
    }

    // GET /api/v1/pagos/pedido/{pedidoId} → 200
    @Test
    void deberiaRetornarPagosPorPedido() throws Exception {
        when(pagoService.obtenerPorPedido(1L)).thenReturn(List.of(pagoEjemplo()));

        mockMvc.perform(get("/api/v1/pagos/pedido/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pagoList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.pagoList[0].estado").value("PROCESANDO"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // GET /api/v1/pagos/pedido/{pedidoId} → 204
    @Test
    void deberiaRetornar204CuandoNohayPagosParaElPedido() throws Exception {
        when(pagoService.obtenerPorPedido(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pagos/pedido/99"))
                .andExpect(status().isNoContent());
    }

    // POST /api/v1/pagos/confirmar → 200
    @Test
    void deberiaConfirmarTransaccionComoAdmin() throws Exception {
        Pago pagoConfirmado = pagoEjemplo();
        pagoConfirmado.setEstado(EstadoPago.COMPLETADO);

        when(pagoService.confirmarTransaccion(eq("abc-123-uuid"), eq("SUCCESS"), eq("ADMIN")))
                .thenReturn(pagoConfirmado);

        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "abc-123-uuid")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "ADMIN")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        verify(pagoService).confirmarTransaccion("abc-123-uuid", "SUCCESS", "ADMIN");
    }

    // POST /api/v1/pagos/confirmar → 403
    @Test
    void deberiaRetornar403AlConfirmarSiNoEsAdmin() throws Exception {
        when(pagoService.confirmarTransaccion(any(), any(), eq("CLIENTE")))
                .thenThrow(new SecurityException("Solo administradores pueden confirmar pagos"));

        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "abc-123-uuid")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo administradores pueden confirmar pagos"));
    }

    // POST /api/v1/pagos/confirmar → 404
    @Test
    void deberiaRetornar404CuandoTransaccionNoExiste() throws Exception {
        when(pagoService.confirmarTransaccion(eq("no-existe"), eq("SUCCESS"), eq("ADMIN")))
                .thenThrow(new RuntimeException("Pago no encontrado con transaccionId: no-existe"));

        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "no-existe")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Pago no encontrado con transaccionId: no-existe"));
    }

    // GET /api/v1/pagos/health → 200
    @Test
    void deberiaRetornarHealthOk() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-pagos"))
                .andExpect(jsonPath("$.estado").value("activo"));
    }
}