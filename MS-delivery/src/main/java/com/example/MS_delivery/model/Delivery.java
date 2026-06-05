package com.example.MS_delivery.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID del pedido en MS-pedidos (no FK porque es un microservicio externo)
    @NotNull(message = "El pedidoId es obligatorio")
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Repartidor repartidor;

    @NotBlank(message = "La dirección destino es obligatoria")
    @Column(name = "direccion_destino", nullable = false)
    private String direccionDestino;

    @Column(name = "latitud_destino")
    private Double latitudDestino;

    @Column(name = "longitud_destino")
    private Double longitudDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoDelivery estado = EstadoDelivery.PENDIENTE;

    @Builder.Default
    @Column(nullable = false)
    private Integer intentos = 0;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @Column(name = "fecha_inicio_ruta")
    private LocalDateTime fechaInicioRuta;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    // ── Enum ────────────────────────────────────────────────────────────────

    public enum EstadoDelivery { PENDIENTE, EN_RUTA, ENTREGADO, FALLIDO, CANCELADO }
}
