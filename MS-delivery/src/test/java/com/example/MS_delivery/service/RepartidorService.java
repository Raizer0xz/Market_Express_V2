package com.example.MS_delivery.service;

import com.example.MS_delivery.dto.DeliveryDtos.MetricasRepartidorResponse;
import com.example.MS_delivery.dto.DeliveryDtos.RepartidorRequest;
import com.example.MS_delivery.dto.DeliveryDtos.UbicacionRequest;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.repository.DeliveryRepository;
import com.example.MS_delivery.repository.RepartidorRepository;
import com.example.MS_delivery.repository.UbicacionHistorialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepartidorService {

    private final RepartidorRepository          repartidorRepository;
    private final UbicacionHistorialRepository  ubicacionRepository;
    private final DeliveryRepository            deliveryRepository;

    @Value("${app.delivery.radio-busqueda-km:10.0}")
    private double radioBusquedaKm;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Repartidor> listarActivos() {
        return repartidorRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Repartidor obtenerPorId(Long id) {
        return repartidorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado: " + id));
    }

    @Transactional
    public Repartidor registrar(RepartidorRequest req) {
        if (repartidorRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Ya existe un repartidor con ese email");
        }
        Repartidor r = Repartidor.builder()
                .nombre(req.getNombre())
                .telefono(req.getTelefono())
                .email(req.getEmail())
                .vehiculo(req.getVehiculo() != null ? req.getVehiculo() : Repartidor.Vehiculo.MOTO)
                .build();
        log.info("Registrando repartidor: {}", r.getNombre());
        return repartidorRepository.save(r);
    }

    @Transactional
    public Repartidor actualizar(Long id, RepartidorRequest req) {
        Repartidor r = obtenerPorId(id);
        r.setNombre(req.getNombre());
        r.setTelefono(req.getTelefono());
        if (req.getVehiculo() != null) r.setVehiculo(req.getVehiculo());
        log.info("Actualizando repartidor: {}", id);
        return repartidorRepository.save(r);
    }

    @Transactional
    public void desactivar(Long id) {
        Repartidor r = obtenerPorId(id);
        r.setActivo(false);
        r.setEstado(EstadoRepartidor.INACTIVO);
        repartidorRepository.save(r);
        log.info("Repartidor {} desactivado", id);
    }

    @Transactional
    public void cambiarEstado(Long id, EstadoRepartidor nuevoEstado) {
        obtenerPorId(id);
        int filas = repartidorRepository.updateEstado(id, nuevoEstado);
        if (filas == 0) throw new RuntimeException("No se pudo actualizar el estado");
        log.info("Repartidor {} → estado {}", id, nuevoEstado);
    }

    // ── GPS ──────────────────────────────────────────────────────────────────

    /**
     * Llamado desde la app del repartidor en cada ping GPS.
     * 1. Actualiza la posición actual en repartidor (sobreescribe).
     * 2. Inserta un punto en ubicacion_historial.
     */
    @Transactional
    public void actualizarUbicacion(Long repartidorId, UbicacionRequest req) {
        Repartidor r = obtenerPorId(repartidorId);

        // 1. Sobreescribir posición actual
        repartidorRepository.updateUbicacion(repartidorId, req.getLatitud(), req.getLongitud());

        // 2. Guardar en historial — asociar al delivery activo si existe
        UbicacionHistorial punto = UbicacionHistorial.builder()
                .repartidor(r)
                .latitud(req.getLatitud())
                .longitud(req.getLongitud())
                .velocidadKmh(req.getVelocidadKmh())
                .precisionM(req.getPrecisionM())
                .build();

        deliveryRepository.findActivoByRepartidorId(repartidorId)
                .ifPresent(punto::setDelivery);

        ubicacionRepository.save(punto);
    }

    // ── Selección automática ─────────────────────────────────────────────────

    /**
     * Devuelve el repartidor LIBRE más cercano al destino del pedido.
     * Usado por DeliveryService al asignar automáticamente.
     */
    @Transactional(readOnly = true)
    public Repartidor seleccionarMejorRepartidor(double latDestino, double lngDestino) {
        List<Repartidor> candidatos =
                repartidorRepository.findLibresCercanos(latDestino, lngDestino, radioBusquedaKm);

        if (candidatos.isEmpty()) {
            throw new RuntimeException(
                "No hay repartidores disponibles en un radio de " + radioBusquedaKm + " km");
        }
        return candidatos.get(0); // ya viene ordenado por distancia ASC
    }

    // ── Métricas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MetricasRepartidorResponse obtenerMetricas(Long id) {
        Repartidor r = obtenerPorId(id);
        List<Delivery> todos = deliveryRepository.findByRepartidorIdOrderByFechaAsignacionDesc(id);

        long exitosas = todos.stream()
                .filter(d -> d.getEstado() == Delivery.EstadoDelivery.ENTREGADO).count();
        long fallidas = todos.stream()
                .filter(d -> d.getEstado() == Delivery.EstadoDelivery.FALLIDO).count();
        double tasa = todos.isEmpty() ? 0.0 : (exitosas * 100.0) / todos.size();

        return MetricasRepartidorResponse.builder()
                .repartidorId(id)
                .nombre(r.getNombre())
                .totalEntregas(todos.size())
                .entregasExitosas(exitosas)
                .entregasFallidas(fallidas)
                .tasaExito(Math.round(tasa * 10.0) / 10.0)
                .build();
    }
}
