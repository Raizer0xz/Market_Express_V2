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

import java.math.BigDecimal;
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

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/procesar → 201 con links HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaProcesarPagoYRetornar201() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class))).thenReturn(pagoEjemplo());

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
                .andExpect(jsonPath("$.pedidoId").value(1))
                .andExpect(jsonPath("$.estado").value("PROCESANDO"))
                .andExpect(jsonPath("$.metodo").value("TARJETA_CREDITO"))
                .andExpect(jsonPath("$._links.pagos-del-pedido.href").exists())
                .andExpect(jsonPath("$._links.metodos-disponibles.href").exists())
                .andExpect(jsonPath("$._links.confirmar.href").exists());

        verify(pagoService).procesarPago(any(PagoRequest.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/procesar → 403 rol no autorizado
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar403CuandoRolNoEsClienteNiAdmin() throws Exception {
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

        verify(pagoService, never()).procesarPago(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/procesar → 400 método de pago inválido
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoMetodoDePagoEsInvalido() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class)))
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

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/procesar → 400 campos obligatorios faltantes
    // -------------------------------------------------------------------------
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

        verify(pagoService, never()).procesarPago(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/metodos → 200 lista de métodos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarMetodosDePagoDisponibles() throws Exception {
        when(pagoService.obtenerMetodosDisponibles())
                .thenReturn(List.of(
                        MetodoPago.TARJETA_CREDITO,
                        MetodoPago.TARJETA_DEBITO,
                        MetodoPago.TRANSFERENCIA_BANCARIA,
                        MetodoPago.PAYPAL
                ));

        mockMvc.perform(get("/api/v1/pagos/metodos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("TARJETA_CREDITO"))
                .andExpect(jsonPath("$[1]").value("TARJETA_DEBITO"))
                .andExpect(jsonPath("$[2]").value("TRANSFERENCIA_BANCARIA"))
                .andExpect(jsonPath("$[3]").value("PAYPAL"));

        verify(pagoService).obtenerMetodosDisponibles();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/pedido/{pedidoId} → 200 con lista + HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarPagosPorPedido() throws Exception {
        when(pagoService.obtenerPorPedido(1L)).thenReturn(List.of(pagoEjemplo()));

        mockMvc.perform(get("/api/v1/pagos/pedido/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pagoList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.pagoList[0].pedidoId").value(1))
                .andExpect(jsonPath("$._embedded.pagoList[0].estado").value("PROCESANDO"))
                .andExpect(jsonPath("$._embedded.pagoList[0]._links.pagos-del-pedido.href").exists())
                .andExpect(jsonPath("$._embedded.pagoList[0]._links.confirmar.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(pagoService).obtenerPorPedido(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/pedido/{pedidoId} → 204 sin pagos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoNohayPagosParaElPedido() throws Exception {
        when(pagoService.obtenerPorPedido(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pagos/pedido/99"))
                .andExpect(status().isNoContent());

        verify(pagoService).obtenerPorPedido(99L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/confirmar → 200 confirmado + links
    // -------------------------------------------------------------------------
    @Test
    void deberiaConfirmarTransaccionComoAdmin() throws Exception {
        Pago pagoConfirmado = pagoEjemplo();
        pagoConfirmado.setEstado(EstadoPago.COMPLETADO);

        when(pagoService.confirmarTransaccion(eq("abc-123-uuid"), eq("SUCCESS")))
                .thenReturn(pagoConfirmado);

        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "abc-123-uuid")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "ADMIN")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$._links.pagos-del-pedido.href").exists())
                .andExpect(jsonPath("$._links.confirmar.href").exists());

        verify(pagoService).confirmarTransaccion("abc-123-uuid", "SUCCESS");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/confirmar → 403 no es ADMIN
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar403AlConfirmarSiNoEsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "abc-123-uuid")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo administradores pueden confirmar pagos"));

        verify(pagoService, never()).confirmarTransaccion(any(), any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/confirmar → 404 transacción no existe
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoTransaccionNoExiste() throws Exception {
        when(pagoService.confirmarTransaccion(eq("no-existe"), eq("SUCCESS")))
                .thenThrow(new RuntimeException("Pago no encontrado con transaccionId: no-existe"));

        mockMvc.perform(post("/api/v1/pagos/confirmar")
                        .param("transaccionId", "no-existe")
                        .param("status", "SUCCESS")
                        .header("X-Usuario-Rol", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Pago no encontrado con transaccionId: no-existe"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/health → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHealthOk() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-pagos"))
                .andExpect(jsonPath("$.estado").value("activo"))
                .andExpect(jsonPath("$.puerto").value("9093"));
    }
}