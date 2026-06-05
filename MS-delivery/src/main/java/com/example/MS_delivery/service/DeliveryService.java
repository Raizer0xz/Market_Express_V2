package com.example.MS_delivery.service;

import com.example.MS_delivery.client.FeignClients.PedidoClient;
import com.example.MS_delivery.client.FeignClients.PedidoResponse;
import com.example.MS_delivery.dto.DeliveryDtos.IniciarDeliveryRequest;
import com.example.MS_delivery.dto.DeliveryDtos.UbicacionResponse;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.repository.DeliveryRepository;
import com.example.MS_delivery.repository.RepartidorRepository;
import com.example.MS_delivery.repository.UbicacionHistorialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository        deliveryRepository;
    private final RepartidorRepository      repartidorRepository;
    private final UbicacionHistorialRepository ubicacionRepository;
    private final RepartidorService         repartidorService;
    private final PedidoClient              pedidoClient;

    // ── Asignación automática ────────────────────────────────────────────────

    /**
     * Crea un Delivery y asigna automáticamente al repartidor LIBRE más cercano.
     * Flujo:
     *   1. Validar que el pedido no tenga ya un delivery activo.
     *   2. Consultar MS-pedidos para obtener dirección/coordenadas.
     *   3. Seleccionar repartidor más cercano (Haversine).
     *   4. Crear Delivery en estado PENDIENTE.
     *   5. Marcar repartidor como OCUPADO.
     */
    @Transactional
    public Delivery asignarAutomaticamente(IniciarDeliveryRequest req) {
        // 1. ¿Ya existe un delivery activo para este pedido?
        if (deliveryRepository.existsActivoByPedidoId(req.getPedidoId())) {
            throw new RuntimeException(
                "El pedido " + req.getPedidoId() + " ya tiene un delivery en curso");
        }

        // 2. Enriquecer con datos del pedido (via Feign)
        String direccion   = req.getDireccionDestino();
        Double latDestino  = req.getLatitudDestino();
        Double lngDestino  = req.getLongitudDestino();

        try {
            PedidoResponse pedido = pedidoClient.obtenerPedido(req.getPedidoId());
            if (pedido.getDireccionEntrega() != null) direccion  = pedido.getDireccionEntrega();
            if (pedido.getLatitudEntrega()   != null) latDestino = pedido.getLatitudEntrega();
            if (pedido.getLongitudEntrega()  != null) lngDestino = pedido.getLongitudEntrega();
        } catch (Exception e) {
            log.warn("No se pudo consultar MS-pedidos para pedido {}: {}", req.getPedidoId(), e.getMessage());
            // Se continúa con los datos del request si Feign falla
        }

        // 3. Elegir el mejor repartidor
        if (latDestino == null || lngDestino == null) {
            throw new RuntimeException(
                "Se necesitan coordenadas del destino para asignación automática");
        }
        Repartidor repartidor = repartidorService.seleccionarMejorRepartidor(latDestino, lngDestino);

        // 4. Crear delivery
        Delivery delivery = Delivery.builder()
                .pedidoId(req.getPedidoId())
                .repartidor(repartidor)
                .direccionDestino(direccion)
                .latitudDestino(latDestino)
                .longitudDestino(lngDestino)
                .estado(EstadoDelivery.PENDIENTE)
                .build();

        deliveryRepository.save(delivery);

        // 5. Marcar repartidor OCUPADO
        repartidorRepository.updateEstado(repartidor.getId(), EstadoRepartidor.OCUPADO);

        log.info("Delivery {} asignado al repartidor {} para pedido {}",
                delivery.getId(), repartidor.getId(), req.getPedidoId());
        return delivery;
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Transactional
    public Delivery iniciarRuta(Long deliveryId) {
        Delivery d = obtenerPorId(deliveryId);
        validarEstado(d, EstadoDelivery.PENDIENTE, "iniciar ruta");
        d.setEstado(EstadoDelivery.EN_RUTA);
        d.setFechaInicioRuta(LocalDateTime.now());
        log.info("Delivery {} → EN_RUTA", deliveryId);
        return deliveryRepository.save(d);
    }

    @Transactional
    public Delivery confirmarEntrega(Long deliveryId) {
        Delivery d = obtenerPorId(deliveryId);
        validarEstado(d, EstadoDelivery.EN_RUTA, "confirmar entrega");
        d.setEstado(EstadoDelivery.ENTREGADO);
        d.setFechaEntrega(LocalDateTime.now());
        // Liberar repartidor
        repartidorRepository.updateEstado(d.getRepartidor().getId(), EstadoRepartidor.LIBRE);
        log.info("Delivery {} ENTREGADO — repartidor {} liberado",
                deliveryId, d.getRepartidor().getId());
        return deliveryRepository.save(d);
    }

    @Transactional
    public Delivery reportarFallo(Long deliveryId, String notas) {
        Delivery d = obtenerPorId(deliveryId);
        if (d.getEstado() == EstadoDelivery.ENTREGADO || d.getEstado() == EstadoDelivery.CANCELADO) {
            throw new RuntimeException("No se puede reportar fallo en estado " + d.getEstado());
        }
        d.setEstado(EstadoDelivery.FALLIDO);
        d.setNotas(notas);
        d.setIntentos(d.getIntentos() + 1);
        // Liberar repartidor para que pueda tomar otro pedido
        repartidorRepository.updateEstado(d.getRepartidor().getId(), EstadoRepartidor.LIBRE);
        log.warn("Delivery {} FALLIDO — intento #{}", deliveryId, d.getIntentos());
        return deliveryRepository.save(d);
    }

    @Transactional
    public Delivery cancelar(Long deliveryId, String notas) {
        Delivery d = obtenerPorId(deliveryId);
        if (d.getEstado() == EstadoDelivery.ENTREGADO) {
            throw new RuntimeException("No se puede cancelar un delivery ya entregado");
        }
        d.setEstado(EstadoDelivery.CANCELADO);
        d.setNotas(notas);
        repartidorRepository.updateEstado(d.getRepartidor().getId(), EstadoRepartidor.LIBRE);
        log.info("Delivery {} CANCELADO", deliveryId);
        return deliveryRepository.save(d);
    }

    // ── Ubicación en tiempo real (lo que consulta el cliente) ────────────────

    /**
     * Endpoint público para el cliente: devuelve la posición actual del
     * repartidor asignado a su pedido, más el estado del delivery y un ETA
     * estimado (distancia / 30 km/h promedio, redondeo a minutos).
     */
    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacionPorPedido(Long pedidoId) {
        Delivery delivery = deliveryRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RuntimeException(
                    "No hay delivery activo para el pedido " + pedidoId));

        Repartidor rep = delivery.getRepartidor();

        // La posición actual está en la columna repartidor.latitud / longitud
        Double latRep = rep.getLatitud();
        Double lngRep = rep.getLongitud();

        // Calcular ETA si tenemos ambas coordenadas
        Integer etaMinutos = null;
        if (latRep != null && lngRep != null
                && delivery.getLatitudDestino()  != null
                && delivery.getLongitudDestino() != null) {
            double distanciaKm = haversineKm(
                    latRep, lngRep,
                    delivery.getLatitudDestino(), delivery.getLongitudDestino());
            // 30 km/h promedio para repartidores urbanos
            etaMinutos = (int) Math.ceil((distanciaKm / 30.0) * 60);
        }

        return UbicacionResponse.builder()
                .repartidorId(rep.getId())
                .nombreRepartidor(rep.getNombre())
                .latitud(latRep)
                .longitud(lngRep)
                .ultimaActualizacion(rep.getUltimaUbicacion())
                .estadoDelivery(delivery.getEstado())
                .etaMinutos(etaMinutos)
                .build();
    }

    /**
     * Ruta completa recorrida por el repartidor en un delivery.
     * Útil para mostrar el trayecto al cliente una vez entregado.
     */
    @Transactional(readOnly = true)
    public List<UbicacionHistorial> obtenerRutaDelivery(Long deliveryId) {
        obtenerPorId(deliveryId); // valida existencia
        return ubicacionRepository.findByDeliveryIdOrderByTimestampAsc(deliveryId);
    }

    // ── Consultas generales ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Delivery obtenerPorId(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Delivery obtenerPorPedidoId(Long pedidoId) {
        return deliveryRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RuntimeException(
                    "Delivery no encontrado para pedido: " + pedidoId));
    }

    @Transactional(readOnly = true)
    public List<Delivery> historialRepartidor(Long repartidorId) {
        return deliveryRepository.findByRepartidorIdOrderByFechaAsignacionDesc(repartidorId);
    }

    @Transactional(readOnly = true)
    public List<Delivery> listarPorEstado(EstadoDelivery estado) {
        return deliveryRepository.findByEstado(estado);
    }

    // ── Utils ────────────────────────────────────────────────────────────────

    private void validarEstado(Delivery d, EstadoDelivery esperado, String operacion) {
        if (d.getEstado() != esperado) {
            throw new RuntimeException(
                "No se puede '" + operacion + "' con estado " + d.getEstado()
                + ". Se requiere: " + esperado);
        }
    }

    /**
     * Fórmula de Haversine — distancia en km entre dos coordenadas.
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
