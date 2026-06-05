package com.example.MS_delivery.repository;

import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByPedidoId(Long pedidoId);

    List<Delivery> findByRepartidorId(Long repartidorId);

    List<Delivery> findByEstado(EstadoDelivery estado);

    /**
     * Delivery activo (PENDIENTE o EN_RUTA) de un repartidor.
     * Un repartidor no puede tener más de uno al mismo tiempo.
     * Usado por RepartidorService al guardar puntos GPS en historial.
     */
    @Query("""
        SELECT d FROM Delivery d
        WHERE d.repartidor.id = :repartidorId
          AND d.estado IN ('PENDIENTE', 'EN_RUTA')
        ORDER BY d.fechaAsignacion DESC
        """)
    Optional<Delivery> findActivoByRepartidorId(@Param("repartidorId") Long repartidorId);

    /**
     * ¿El pedido ya tiene un delivery no cancelado/fallido?
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM Delivery d
        WHERE d.pedidoId = :pedidoId
          AND d.estado NOT IN ('CANCELADO', 'FALLIDO')
        """)
    boolean existsActivoByPedidoId(@Param("pedidoId") Long pedidoId);

    /** Historial completo de un repartidor ordenado por fecha desc. */
    List<Delivery> findByRepartidorIdOrderByFechaAsignacionDesc(Long repartidorId);

    /** Conteo de entregas exitosas de un repartidor. */
    @Query("""
        SELECT COUNT(d) FROM Delivery d
        WHERE d.repartidor.id = :repartidorId
          AND d.estado = 'ENTREGADO'
        """)
    long countEntregasByRepartidorId(@Param("repartidorId") Long repartidorId);
}
