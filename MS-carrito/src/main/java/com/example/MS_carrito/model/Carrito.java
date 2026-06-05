package com.example.MS_carrito.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CARRITO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK a ms-usuarios — solo guardamos el ID
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // FK a ms-sucursales — solo guardamos el ID
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    // Estados: ACTIVO, CONFIRMADO, ABANDONADO
    private String estado = "ACTIVO";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relación 1:N — un carrito tiene muchos ítems
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items;
}