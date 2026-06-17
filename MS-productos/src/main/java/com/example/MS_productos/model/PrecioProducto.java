package com.example.MS_productos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "precio_producto")
@NoArgsConstructor
@AllArgsConstructor
public class PrecioProducto extends RepresentationModel<PrecioProducto> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull(message = "La sucursal es obligatoria")
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Positive(message = "El precio debe ser mayor a 0")
    @Column(nullable = false)
    private BigDecimal precio;

    @CreationTimestamp
    @Column(name = "vigente_desde", updatable = false)
    private LocalDateTime vigenteDesde;
}