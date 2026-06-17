package com.example.MS_pagos.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago extends RepresentationModel<Pago> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(unique = true)
    private String transaccionId;

    private Double monto;
    private String moneda;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private String confirmacionHash;

    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}