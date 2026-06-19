package com.example.MS_pagos.service;

import com.example.MS_pagos.modelo.EstadoPago;
import com.example.MS_pagos.modelo.MetodoPago;
import com.example.MS_pagos.modelo.Pago;
import com.example.MS_pagos.modelo.PagoRequest;
import com.example.MS_pagos.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    private Pago pago;
    private PagoRequest request;

    @BeforeEach
    void setUp() {
        request = PagoRequest.builder()
                .pedidoId(1L)
                .monto(new BigDecimal("15000.00"))
                .moneda("CLP")
                .metodo("TARJETA_CREDITO")
                .build();

        pago = Pago.builder()
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
    // procesarPago()
    // -------------------------------------------------------------------------
    @Test
    void procesarPago_deberiaCrearPagoConEstadoProcesando() {
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);

        Pago resultado = pagoService.procesarPago(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoPago.PROCESANDO);
        assertThat(resultado.getMetodo()).isEqualTo(MetodoPago.TARJETA_CREDITO);
        assertThat(resultado.getPedidoId()).isEqualTo(1L);
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void procesarPago_deberiaUsarCLPCuandoMonedaEsNull() {
        request.setMoneda(null);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            assertThat(p.getMoneda()).isEqualTo("CLP");
            return pago;
        });

        pagoService.procesarPago(request);

        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void procesarPago_deberiaLanzarExcepcionCuandoMetodoEsInvalido() {
        request.setMetodo("EFECTIVO_INVALIDO");

        assertThatThrownBy(() -> pagoService.procesarPago(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Metodo de pago invalido");

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void procesarPago_deberiaAceptarTodosLosMetodosValidos() {
        for (MetodoPago metodo : MetodoPago.values()) {
            request.setMetodo(metodo.name());
            Pago pagoMetodo = Pago.builder()
                    .id(1L).pedidoId(1L).monto(15000.00)
                    .moneda("CLP").metodo(metodo)
                    .estado(EstadoPago.PROCESANDO)
                    .transaccionId("uuid-" + metodo.name())
                    .build();

            when(pagoRepository.save(any(Pago.class))).thenReturn(pagoMetodo);

            Pago resultado = pagoService.procesarPago(request);
            assertThat(resultado.getMetodo()).isEqualTo(metodo);
        }
    }

    // -------------------------------------------------------------------------
    // obtenerMetodosDisponibles()
    // -------------------------------------------------------------------------
    @Test
    void obtenerMetodosDisponibles_deberiaRetornarTodosLosMetodos() {
        List<MetodoPago> metodos = pagoService.obtenerMetodosDisponibles();

        assertThat(metodos).hasSize(MetodoPago.values().length);
        assertThat(metodos).contains(
                MetodoPago.TARJETA_CREDITO,
                MetodoPago.TARJETA_DEBITO,
                MetodoPago.TRANSFERENCIA_BANCARIA,
                MetodoPago.PAYPAL
        );
    }

    // -------------------------------------------------------------------------
    // obtenerPorPedido()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorPedido_deberiaRetornarPagosDElPedido() {
        when(pagoRepository.findByPedidoId(1L)).thenReturn(List.of(pago));

        List<Pago> resultado = pagoService.obtenerPorPedido(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPedidoId()).isEqualTo(1L);
        verify(pagoRepository).findByPedidoId(1L);
    }

    @Test
    void obtenerPorPedido_deberiaRetornarListaVaciaSiNohayPagos() {
        when(pagoRepository.findByPedidoId(99L)).thenReturn(List.of());

        List<Pago> resultado = pagoService.obtenerPorPedido(99L);

        assertThat(resultado).isEmpty();
        verify(pagoRepository).findByPedidoId(99L);
    }

    // -------------------------------------------------------------------------
    // confirmarTransaccion()
    // -------------------------------------------------------------------------
    @Test
    void confirmarTransaccion_deberiaPonerEstadoCOMPLETADOCuandoStatusEsSUCCESS() {
        when(pagoRepository.findByTransaccionId("abc-123-uuid")).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);

        Pago resultado = pagoService.confirmarTransaccion("abc-123-uuid", "SUCCESS");

        assertThat(resultado.getEstado()).isEqualTo(EstadoPago.COMPLETADO);
        verify(pagoRepository).findByTransaccionId("abc-123-uuid");
        verify(pagoRepository).save(pago);
    }

    @Test
    void confirmarTransaccion_deberiaPonerEstadoRECHAZADOCuandoStatusEsFAILED() {
        when(pagoRepository.findByTransaccionId("abc-123-uuid")).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);

        Pago resultado = pagoService.confirmarTransaccion("abc-123-uuid", "FAILED");

        assertThat(resultado.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        verify(pagoRepository).save(pago);
    }

    @Test
    void confirmarTransaccion_deberiaLanzarExcepcionCuandoTransaccionNoExiste() {
        when(pagoRepository.findByTransaccionId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.confirmarTransaccion("no-existe", "SUCCESS"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pago no encontrado con transaccionId: no-existe");

        verify(pagoRepository, never()).save(any());
    }
}