package com.example.MS_delivery.service;

import com.example.MS_delivery.client.FeignClients.PedidoClient;
import com.example.MS_delivery.dto.DeliveryDtos.IniciarDeliveryRequest;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.repository.DeliveryRepository;
import com.example.MS_delivery.repository.RepartidorRepository;
import com.example.MS_delivery.repository.UbicacionHistorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private RepartidorRepository repartidorRepository;
    @Mock private UbicacionHistorialRepository ubicacionRepository;
    @Mock private RepartidorService repartidorService;
    @Mock private PedidoClient pedidoClient;

    @InjectMocks
    private DeliveryService deliveryService;

    private Repartidor repartidor;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        repartidor = Repartidor.builder()
                .id(3L).nombre("Carlos Pérez").email("carlos@mail.com")
                .telefono("987654321").vehiculo(Repartidor.Vehiculo.MOTO)
                .estado(Repartidor.EstadoRepartidor.LIBRE).activo(true)
                .latitud(-33.45).longitud(-70.66).build();

        delivery = Delivery.builder()
                .id(1L).pedidoId(42L).repartidor(repartidor)
                .direccionDestino("Av. Siempre Viva 123")
                .latitudDestino(-33.4489).longitudDestino(-70.6693)
                .estado(EstadoDelivery.PENDIENTE).intentos(0).build();
    }

    // -------------------------------------------------------------------------
    // asignarAutomaticamente()
    // -------------------------------------------------------------------------
    @Test
    void asignar_deberiaCrearDeliveryYAsignarRepartidor() {
        IniciarDeliveryRequest req = IniciarDeliveryRequest.builder()
                .pedidoId(42L).direccionDestino("Av. Siempre Viva 123")
                .latitudDestino(-33.4489).longitudDestino(-70.6693).build();

        when(deliveryRepository.existsActivoByPedidoId(42L)).thenReturn(false);
        when(pedidoClient.obtenerPedido(42L)).thenThrow(new RuntimeException("Feign error")); // simula fallo gracioso
        when(repartidorService.seleccionarMejorRepartidor(-33.4489, -70.6693)).thenReturn(repartidor);
        when(deliveryRepository.save(any())).thenReturn(delivery);
        when(repartidorRepository.updateEstado(any(), any())).thenReturn(1);

        Delivery resultado = deliveryService.asignarAutomaticamente(req);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getPedidoId()).isEqualTo(42L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoDelivery.PENDIENTE);
        verify(deliveryRepository).save(any());
        verify(repartidorRepository).updateEstado(eq(3L), eq(Repartidor.EstadoRepartidor.OCUPADO));
    }

    @Test
    void asignar_deberiaLanzarExcepcionSiPedidoYaTieneDeliveryActivo() {
        IniciarDeliveryRequest req = IniciarDeliveryRequest.builder()
                .pedidoId(42L).direccionDestino("X")
                .latitudDestino(-33.0).longitudDestino(-70.0).build();

        when(deliveryRepository.existsActivoByPedidoId(42L)).thenReturn(true);

        assertThatThrownBy(() -> deliveryService.asignarAutomaticamente(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya tiene un delivery en curso");

        verify(deliveryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // obtenerPorId()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorId_deberiaRetornarDeliveryCuandoExiste() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        Delivery resultado = deliveryService.obtenerPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getPedidoId()).isEqualTo(42L);
        verify(deliveryRepository).findById(1L);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionCuandoNoExiste() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.obtenerPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Delivery no encontrado: 99");
    }

    // -------------------------------------------------------------------------
    // iniciarRuta()
    // -------------------------------------------------------------------------
    @Test
    void iniciarRuta_deberiaActualizarEstadoAEnRuta() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Delivery resultado = deliveryService.iniciarRuta(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoDelivery.EN_RUTA);
        assertThat(resultado.getFechaInicioRuta()).isNotNull();
        verify(deliveryRepository).save(any());
    }

    @Test
    void iniciarRuta_deberiaLanzarExcepcionSiNoEstaPendiente() {
        delivery.setEstado(EstadoDelivery.ENTREGADO);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.iniciarRuta(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("iniciar ruta");

        verify(deliveryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // confirmarEntrega()
    // -------------------------------------------------------------------------
    @Test
    void confirmarEntrega_deberiaActualizarEstadoYLiberarRepartidor() {
        delivery.setEstado(EstadoDelivery.EN_RUTA);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repartidorRepository.updateEstado(any(), any())).thenReturn(1);

        Delivery resultado = deliveryService.confirmarEntrega(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoDelivery.ENTREGADO);
        assertThat(resultado.getFechaEntrega()).isNotNull();
        verify(repartidorRepository).updateEstado(eq(3L), eq(Repartidor.EstadoRepartidor.LIBRE));
    }

    @Test
    void confirmarEntrega_deberiaLanzarExcepcionSiNoEstaEnRuta() {
        delivery.setEstado(EstadoDelivery.PENDIENTE);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.confirmarEntrega(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("confirmar entrega");
    }

    // -------------------------------------------------------------------------
    // reportarFallo()
    // -------------------------------------------------------------------------
    @Test
    void reportarFallo_deberiaActualizarEstadoYLiberarRepartidor() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repartidorRepository.updateEstado(any(), any())).thenReturn(1);

        Delivery resultado = deliveryService.reportarFallo(1L, "Cliente ausente");

        assertThat(resultado.getEstado()).isEqualTo(EstadoDelivery.FALLIDO);
        assertThat(resultado.getNotas()).isEqualTo("Cliente ausente");
        assertThat(resultado.getIntentos()).isEqualTo(1);
        verify(repartidorRepository).updateEstado(eq(3L), eq(Repartidor.EstadoRepartidor.LIBRE));
    }

    @Test
    void reportarFallo_deberiaLanzarExcepcionSiDeliveryYaFueEntregado() {
        delivery.setEstado(EstadoDelivery.ENTREGADO);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.reportarFallo(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede reportar fallo en estado ENTREGADO");
    }

    // -------------------------------------------------------------------------
    // cancelar()
    // -------------------------------------------------------------------------
    @Test
    void cancelar_deberiaActualizarEstadoYLiberarRepartidor() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repartidorRepository.updateEstado(any(), any())).thenReturn(1);

        Delivery resultado = deliveryService.cancelar(1L, "Cliente canceló");

        assertThat(resultado.getEstado()).isEqualTo(EstadoDelivery.CANCELADO);
        assertThat(resultado.getNotas()).isEqualTo("Cliente canceló");
        verify(repartidorRepository).updateEstado(eq(3L), eq(Repartidor.EstadoRepartidor.LIBRE));
    }

    @Test
    void cancelar_deberiaLanzarExcepcionSiDeliveryYaFueEntregado() {
        delivery.setEstado(EstadoDelivery.ENTREGADO);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelar(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede cancelar un delivery ya entregado");
    }

    // -------------------------------------------------------------------------
    // listarPorEstado()
    // -------------------------------------------------------------------------
    @Test
    void listarPorEstado_deberiaRetornarDeliveriesConEseEstado() {
        when(deliveryRepository.findByEstado(EstadoDelivery.EN_RUTA)).thenReturn(List.of(delivery));

        List<Delivery> resultado = deliveryService.listarPorEstado(EstadoDelivery.EN_RUTA);

        assertThat(resultado).hasSize(1);
        verify(deliveryRepository).findByEstado(EstadoDelivery.EN_RUTA);
    }

    // -------------------------------------------------------------------------
    // obtenerRutaDelivery()
    // -------------------------------------------------------------------------
    @Test
    void obtenerRutaDelivery_deberiaRetornarPuntosGPS() {
        UbicacionHistorial punto = UbicacionHistorial.builder()
                .id(1L).latitud(-33.45).longitud(-70.66).velocidadKmh(25.0).build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(ubicacionRepository.findByDeliveryIdOrderByTimestampAsc(1L)).thenReturn(List.of(punto));

        List<UbicacionHistorial> resultado = deliveryService.obtenerRutaDelivery(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLatitud()).isEqualTo(-33.45);
        verify(ubicacionRepository).findByDeliveryIdOrderByTimestampAsc(1L);
    }
}