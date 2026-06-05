package com.example.MS_carrito.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ITEM_CARRITO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación N:1 — muchos ítems pertenecen a un carrito
    @ManyToOne
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    // FK a ms-productos — solo guardamos el ID
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @Positive
    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;
}