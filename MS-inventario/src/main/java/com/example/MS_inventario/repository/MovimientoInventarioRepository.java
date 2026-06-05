package com.example.MS_inventario.repository;

import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /** Historial de movimientos de un producto en una sucursal */
    List<MovimientoInventario> findByProductoIdAndSucursalIdOrderByCreatedAtDesc(
            Long productoId, Long sucursalId);

    /** Todos los movimientos de una sucursal, mas reciente primero */
    List<MovimientoInventario> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);

    /** Movimientos por tipo (ENTRADA, SALIDA, AJUSTE) en una sucursal */
    List<MovimientoInventario> findBySucursalIdAndTipoOrderByCreatedAtDesc(
            Long sucursalId, TipoMovimiento tipo);
}
