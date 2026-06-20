package com.example.MS_carrito.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;

@Entity
@Table(name = "ITEM_CARRITO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito extends RepresentationModel<ItemCarrito> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @Positive
    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;
}