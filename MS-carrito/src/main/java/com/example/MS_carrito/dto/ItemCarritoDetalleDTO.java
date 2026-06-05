package com.example.MS_carrito.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemCarritoDetalleDTO {
    private Long itemId;
    private Long productoId;
    private String nombreProducto;   // viene de ms-productos
    private String unidadMedida;     // viene de ms-productos
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;     // calculado: precio * cantidad
}