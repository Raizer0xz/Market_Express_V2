package com.example.MS_inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventario",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_producto_sucursal",
                columnNames = {"producto_id", "sucursal_id"}
        )
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Inventario extends RepresentationModel<Inventario> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @NotNull(message = "La sucursal es obligatoria")
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 0;

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