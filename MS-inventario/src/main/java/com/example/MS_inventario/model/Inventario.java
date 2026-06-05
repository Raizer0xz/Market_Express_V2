package com.example.MS_inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Inventario: stock actual de un producto en una sucursal especifica.
 *
 * La combinacion (producto_id, sucursal_id) es unica:
 * no puede haber dos filas para el mismo producto en la misma sucursal.
 *
 * stock_minimo → si la cantidad cae por debajo de este valor,
 *               el producto aparece en las alertas de stock bajo.
 */
@Entity
@Table(
    name = "inventario",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_producto_sucursal",
        columnNames = {"producto_id", "sucursal_id"}
    )
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del producto en MS-productos (relacion logica, sin FK fisica) */
    @NotNull(message = "El producto es obligatorio")
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /** ID de la sucursal en MS-sucursales (relacion logica, sin FK fisica) */
    @NotNull(message = "La sucursal es obligatoria")
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    /** Cantidad actual disponible en esta sucursal */
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 0;

    /**
     * Umbral de alerta: si cantidad <= stockMinimo, el producto
     * aparece en el endpoint de alertas de stock bajo.
     */
    @Min(value = 0)
    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 5;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
