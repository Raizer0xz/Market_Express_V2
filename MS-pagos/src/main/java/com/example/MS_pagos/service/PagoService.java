package com.example.MS_pagos.service;

import com.example.MS_pagos.modelo.EstadoPago;
import com.example.MS_pagos.modelo.MetodoPago;
import com.example.MS_pagos.modelo.Pago;
import com.example.MS_pagos.modelo.PagoRequest;
import com.example.MS_pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;

    @Transactional
    public Pago procesarPago(PagoRequest request) {
        log.info("Iniciando procesamiento de pago para pedido: {} | monto: {} | metodo: {}",
                request.getPedidoId(), request.getMonto(), request.getMetodo());

        // FIX: MetodoPago.valueOf() puede lanzar IllegalArgumentException si el valor
        // no existe en el enum. Se captura y relanza con mensaje claro.
        MetodoPago metodoPago;
        try {
            metodoPago = MetodoPago.valueOf(request.getMetodo().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Metodo de pago invalido: {}", request.getMetodo());
            throw new IllegalArgumentException(
                    "Metodo de pago invalido: '" + request.getMetodo() +
                            "'. Valores aceptados: " + Arrays.toString(MetodoPago.values())
            );
        }

        Pago pago = Pago.builder()
                .pedidoId(request.getPedidoId())
                .monto(request.getMonto().doubleValue()) // compatibilidad con entidad actual
                .moneda(request.getMoneda() != null ? request.getMoneda() : "CLP")
                .metodo(metodoPago)
                .estado(EstadoPago.PROCESANDO)
                .transaccionId(UUID.randomUUID().toString())
                .build();

        Pago guardado = pagoRepository.save(pago);
        log.info("Pago creado exitosamente. TransaccionId: {} | Estado: {}",
                guardado.getTransaccionId(), guardado.getEstado());
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<MetodoPago> obtenerMetodosDisponibles() {
        return Arrays.asList(MetodoPago.values());
    }

    @Transactional(readOnly = true)
    public List<Pago> obtenerPorPedido(Long pedidoId) {
        log.info("Consultando pagos del pedido: {}", pedidoId);
        return pagoRepository.findByPedidoId(pedidoId);
    }

    @Transactional
    public Pago confirmarTransaccion(String transaccionId, String status) {
        log.info("Confirmando transaccion: {} con status: {}", transaccionId, status);
        Pago pago = pagoRepository.findByTransaccionId(transaccionId)
                .orElseThrow(() -> {
                    log.warn("Transaccion no encontrada: {}", transaccionId);
                    return new RuntimeException("Pago no encontrado con transaccionId: " + transaccionId);
                });

        EstadoPago nuevoEstado = "SUCCESS".equalsIgnoreCase(status)
                ? EstadoPago.COMPLETADO
                : EstadoPago.RECHAZADO;

        pago.setEstado(nuevoEstado);
        Pago actualizado = pagoRepository.save(pago);
        log.info("Transaccion {} -> Estado: {}", transaccionId, nuevoEstado);
        return actualizado;
    }
}