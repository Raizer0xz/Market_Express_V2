package com.example.MS_delivery.dto;

import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Repartidor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DeliveryDtos {

    // ────────────────────────────────────────────────────────────────────────
    // REQUEST: crear repartidor
    // ────────────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RepartidorRequest {
        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;
        @NotBlank(message = "El teléfono es obligatorio")
        private String telefono;
        @NotBlank(message = "El email es obligatorio")
        private String email;
        private Repartidor.Vehiculo vehiculo;
    }

    // ────────────────────────────────────────────────────────────────────────
    // REQUEST: iniciar un delivery (lo llama MS-pedidos o un admin)
    // ────────────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IniciarDeliveryRequest {
        @NotNull(message = "El pedidoId es obligatorio")
        private Long pedidoId;
        @NotBlank(message = "La dirección destino es obligatoria")
        private String direccionDestino;
        // Coordenadas del destino para calcular distancia al repartidor
        private Double latitudDestino;
        private Double longitudDestino;
    }

    // ────────────────────────────────────────────────────────────────────────
    // REQUEST: actualizar ubicación GPS (app del repartidor)
    // ────────────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UbicacionRequest {
        @NotNull private Double latitud;
        @NotNull private Double longitud;
        private Double velocidadKmh;
        private Double precisionM;
    }

    // ────────────────────────────────────────────────────────────────────────
    // REQUEST: reportar fallo / notas al cancelar
    // ────────────────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class NotaRequest {
        private String notas;
    }


    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UbicacionResponse {
        private Long repartidorId;
        private String nombreRepartidor;
        private Double latitud;
        private Double longitud;
        private LocalDateTime ultimaActualizacion;
        private Delivery.EstadoDelivery estadoDelivery;
        // ETA estimado en minutos (null si no se pudo calcular)
        private Integer etaMinutos;
    }


    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MetricasRepartidorResponse {
        private Long repartidorId;
        private String nombre;
        private long totalEntregas;
        private long entregasExitosas;
        private long entregasFallidas;
        private Double tasaExito;
    }
}
