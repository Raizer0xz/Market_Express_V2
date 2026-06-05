package com.example.MS_delivery.repository;

import com.example.MS_delivery.model.UbicacionHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UbicacionHistorialRepository extends JpaRepository<UbicacionHistorial, Long> {

    /**
     * Última posición conocida de un repartidor.
     */
    Optional<UbicacionHistorial> findTopByRepartidorIdOrderByTimestampDesc(Long repartidorId);

    /**
     * Ruta completa de un delivery (para mostrar al cliente).
     */
    List<UbicacionHistorial> findByDeliveryIdOrderByTimestampAsc(Long deliveryId);

    /**
     * Historial de un repartidor desde una fecha (últimas N horas, por ej.).
     */
    List<UbicacionHistorial> findByRepartidorIdAndTimestampAfterOrderByTimestampAsc(
            Long repartidorId, LocalDateTime desde);

    /**
     * Limpieza de registros antiguos (llamar desde un @Scheduled).
     */
    @Modifying
    @Query("DELETE FROM UbicacionHistorial u WHERE u.timestamp < :antes")
    int deleteOlderThan(@Param("antes") LocalDateTime antes);
}
