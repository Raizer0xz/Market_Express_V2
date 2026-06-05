package com.example.MS_productos.repository;

import com.example.MS_productos.model.PrecioProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecioProductoRepository extends JpaRepository<PrecioProducto, Long> {

    // Todos los precios de un producto específico
    List<PrecioProducto> findByProductoId(Long productoId);

    // ✅ sucursalId es Long directo, no es una entidad — el nombre debe ser exacto
    List<PrecioProducto> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);
}