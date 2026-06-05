package com.example.MS_inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

/**
 * MovimientoInventario: registro historico de cada cambio de stock.
 *
 * Cada vez que aumenta o disminuye el stock de un producto
 * en una sucursal, se guarda un movimiento aqui.
 * Esto permite auditar quien hizo que y cuando.
 * Solo visible para ADMIN.
 */
@Entity
@Table(name = "movimientos_inventario")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @NotNull
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    /** Tipo de movimiento: ENTRADA, SALIDA o AJUSTE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** Cantidad modificada (siempre positivo; el tipo indica si suma o resta) */
    @Min(value = 1, message = "La cantidad del movimiento debe ser al menos 1")
    @Column(nullable = false)
    private Integer cantidad;

    /** Stock resultante despues de aplicar este movimiento */
    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    /** Descripcion del motivo del movimiento (ej: "Pedido #42", "Ajuste fisico") */
    @Column(length = 300)
    private String motivo;

    /** ID del usuario ADMIN que realizo el movimiento */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
