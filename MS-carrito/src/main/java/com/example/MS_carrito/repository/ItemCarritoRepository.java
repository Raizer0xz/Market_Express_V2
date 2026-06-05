package com.example.MS_carrito.repository;

import com.example.MS_carrito.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    // Todos los ítems de un carrito
    List<ItemCarrito> findByCarritoId(Long carritoId);

    // Buscar ítem específico en un carrito por producto
    Optional<ItemCarrito> findByCarritoIdAndProductoId(Long carritoId, Long productoId);
}