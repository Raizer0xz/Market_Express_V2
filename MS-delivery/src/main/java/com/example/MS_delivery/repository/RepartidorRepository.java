package com.example.MS_delivery.repository;

import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    List<Repartidor> findByActivoTrue();

    List<Repartidor> findByEstadoAndActivoTrue(EstadoRepartidor estado);

    boolean existsByEmail(String email);

    /**
     * Repartidores LIBRES que tienen ubicación registrada,
     * ordenados por distancia usando la fórmula de Haversine.
     * Radio en kilómetros.
     */
    @Query(value = """
        SELECT r.* FROM repartidor r
        WHERE r.estado = 'LIBRE'
          AND r.activo  = TRUE
          AND r.latitud  IS NOT NULL
          AND r.longitud IS NOT NULL
          AND (
            6371 * ACOS(
              COS(RADIANS(:lat)) * COS(RADIANS(r.latitud))
              * COS(RADIANS(r.longitud) - RADIANS(:lng))
              + SIN(RADIANS(:lat)) * SIN(RADIANS(r.latitud))
            )
          ) <= :radioKm
        ORDER BY (
            6371 * ACOS(
              COS(RADIANS(:lat)) * COS(RADIANS(r.latitud))
              * COS(RADIANS(r.longitud) - RADIANS(:lng))
              + SIN(RADIANS(:lat)) * SIN(RADIANS(r.latitud))
            )
        ) ASC
        """, nativeQuery = true)
    List<Repartidor> findLibresCercanos(
            @Param("lat")     double lat,
            @Param("lng")     double lng,
            @Param("radioKm") double radioKm
    );

    /**
     * Actualiza solo el estado (evita cargar toda la entidad).
     */
    @Modifying
    @Query("UPDATE Repartidor r SET r.estado = :estado WHERE r.id = :id")
    int updateEstado(@Param("id") Long id, @Param("estado") EstadoRepartidor estado);

    /**
     * Actualiza posición GPS y timestamp en una sola query.
     */
    @Modifying
    @Query("""
        UPDATE Repartidor r
        SET r.latitud = :lat, r.longitud = :lng,
            r.ultimaUbicacion = CURRENT_TIMESTAMP
        WHERE r.id = :id
        """)
    int updateUbicacion(
            @Param("id")  Long id,
            @Param("lat") double lat,
            @Param("lng") double lng
    );
}
