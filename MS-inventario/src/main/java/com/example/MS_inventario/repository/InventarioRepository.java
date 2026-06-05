package com.example.MS_inventario.repository;

import com.example.MS_inventario.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    /** Stock de un producto en una sucursal especifica */
    Optional<Inventario> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    /** Todo el inventario de una sucursal */
    List<Inventario> findBySucursalId(Long sucursalId);

    /** Todas las sucursales que tienen un producto especifico */
    List<Inventario> findByProductoId(Long productoId);

    /**
     * Productos con stock bajo en alerta: cantidad <= stockMinimo
     * Sirve para el endpoint de alertas globales.
     */
    @Query("SELECT i FROM Inventario i WHERE i.cantidad <= i.stockMinimo")
    List<Inventario> findStockBajo();

    /**
     * Productos con stock bajo en una sucursal especifica
     */
    @Query("SELECT i FROM Inventario i WHERE i.sucursalId = :sucursalId AND i.cantidad <= i.stockMinimo")
    List<Inventario> findStockBajoPorSucursal(Long sucursalId);
}
