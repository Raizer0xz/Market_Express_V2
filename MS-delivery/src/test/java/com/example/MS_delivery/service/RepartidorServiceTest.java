package com.example.MS_delivery.service;

import com.example.MS_delivery.dto.DeliveryDtos.MetricasRepartidorResponse;
import com.example.MS_delivery.dto.DeliveryDtos.RepartidorRequest;
import com.example.MS_delivery.dto.DeliveryDtos.UbicacionRequest;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
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
class RepartidorServiceTest {

    @Mock private RepartidorRepository repartidorRepository;
    @Mock private UbicacionHistorialRepository ubicacionRepository;
    @Mock private DeliveryRepository deliveryRepository;

    @InjectMocks
    private RepartidorService repartidorService;

    private Repartidor repartidor;

    @BeforeEach
    void setUp() {
        repartidor = Repartidor.builder()
                .id(1L).nombre("Carlos Pérez").email("carlos@mail.com")
                .telefono("987654321").vehiculo(Repartidor.Vehiculo.MOTO)
                .estado(EstadoRepartidor.LIBRE).activo(true)
                .latitud(-33.45).longitud(-70.66).build();
    }

    // -------------------------------------------------------------------------
    // listarActivos()
    // -------------------------------------------------------------------------
    @Test
    void listarActivos_deberiaRetornarRepartidoresActivos() {
        when(repartidorRepository.findByActivoTrue()).thenReturn(List.of(repartidor));

        List<Repartidor> resultado = repartidorService.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Carlos Pérez");
        verify(repartidorRepository).findByActivoTrue();
    }

    @Test
    void listarActivos_deberiaRetornarListaVaciaCuandoNoHayActivos() {
        when(repartidorRepository.findByActivoTrue()).thenReturn(List.of());

        List<Repartidor> resultado = repartidorService.listarActivos();

        assertThat(resultado).isEmpty();
        verify(repartidorRepository).findByActivoTrue();
    }

    // -------------------------------------------------------------------------
    // obtenerPorId()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorId_deberiaRetornarRepartidorCuandoExiste() {
        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));

        Repartidor resultado = repartidorService.obtenerPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEmail()).isEqualTo("carlos@mail.com");
        verify(repartidorRepository).findById(1L);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionCuandoNoExiste() {
        when(repartidorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repartidorService.obtenerPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Repartidor no encontrado: 99");
    }

    // -------------------------------------------------------------------------
    // registrar()
    // -------------------------------------------------------------------------
    @Test
    void registrar_deberiaGuardarYRetornarRepartidor() {
        RepartidorRequest req = RepartidorRequest.builder()
                .nombre("Carlos Pérez").telefono("987654321")
                .email("carlos@mail.com").vehiculo(Repartidor.Vehiculo.MOTO).build();

        when(repartidorRepository.existsByEmail("carlos@mail.com")).thenReturn(false);
        when(repartidorRepository.save(any())).thenReturn(repartidor);

        Repartidor resultado = repartidorService.registrar(req);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo("Carlos Pérez");
        verify(repartidorRepository).existsByEmail("carlos@mail.com");
        verify(repartidorRepository).save(any());
    }

    @Test
    void registrar_deberiaLanzarExcepcionCuandoEmailYaExiste() {
        RepartidorRequest req = RepartidorRequest.builder()
                .nombre("Carlos").telefono("987654321").email("carlos@mail.com").build();

        when(repartidorRepository.existsByEmail("carlos@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> repartidorService.registrar(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ya existe un repartidor con ese email");

        verify(repartidorRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // actualizar()
    // -------------------------------------------------------------------------
    @Test
    void actualizar_deberiaActualizarDatosDelRepartidor() {
        RepartidorRequest req = RepartidorRequest.builder()
                .nombre("Carlos Actualizado").telefono("999999999")
                .vehiculo(Repartidor.Vehiculo.AUTO).build();

        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(repartidorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Repartidor resultado = repartidorService.actualizar(1L, req);

        assertThat(resultado.getNombre()).isEqualTo("Carlos Actualizado");
        assertThat(resultado.getTelefono()).isEqualTo("999999999");
        assertThat(resultado.getVehiculo()).isEqualTo(Repartidor.Vehiculo.AUTO);
        verify(repartidorRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // desactivar()
    // -------------------------------------------------------------------------
    @Test
    void desactivar_deberiaPonerActivoEnFalse() {
        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(repartidorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        repartidorService.desactivar(1L);

        assertThat(repartidor.getActivo()).isFalse();
        assertThat(repartidor.getEstado()).isEqualTo(EstadoRepartidor.INACTIVO);
        verify(repartidorRepository).save(repartidor);
    }

    // -------------------------------------------------------------------------
    // cambiarEstado()
    // -------------------------------------------------------------------------
    @Test
    void cambiarEstado_deberiaActualizarEstadoDelRepartidor() {
        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(repartidorRepository.updateEstado(1L, EstadoRepartidor.OCUPADO)).thenReturn(1);

        repartidorService.cambiarEstado(1L, EstadoRepartidor.OCUPADO);

        verify(repartidorRepository).updateEstado(1L, EstadoRepartidor.OCUPADO);
    }

    @Test
    void cambiarEstado_deberiaLanzarExcepcionSiNoSeActualizo() {
        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(repartidorRepository.updateEstado(1L, EstadoRepartidor.LIBRE)).thenReturn(0);

        assertThatThrownBy(() -> repartidorService.cambiarEstado(1L, EstadoRepartidor.LIBRE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No se pudo actualizar el estado");
    }

    // -------------------------------------------------------------------------
    // actualizarUbicacion()
    // -------------------------------------------------------------------------
    @Test
    void actualizarUbicacion_deberiaGuardarPuntoGPS() {
        UbicacionRequest req = UbicacionRequest.builder()
                .latitud(-33.45).longitud(-70.66).velocidadKmh(30.0).precisionM(5.0).build();

        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(repartidorRepository.updateUbicacion(any(), any(), any())).thenReturn(1);
        when(deliveryRepository.findActivoByRepartidorId(1L)).thenReturn(Optional.empty());
        when(ubicacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        repartidorService.actualizarUbicacion(1L, req);

        verify(repartidorRepository).updateUbicacion(1L, -33.45, -70.66);
        verify(ubicacionRepository).save(any(UbicacionHistorial.class));
    }

    // -------------------------------------------------------------------------
    // seleccionarMejorRepartidor()
    // -------------------------------------------------------------------------
    @Test
    void seleccionarMejor_deberiaRetornarRepartidorMasCercano() {
        when(repartidorRepository.findLibresCercanos(any(), any(), any()))
                .thenReturn(List.of(repartidor));

        Repartidor resultado = repartidorService.seleccionarMejorRepartidor(-33.45, -70.66);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(repartidorRepository).findLibresCercanos(any(), any(), any());
    }

    @Test
    void seleccionarMejor_deberiaLanzarExcepcionSiNoHayRepartidores() {
        when(repartidorRepository.findLibresCercanos(any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> repartidorService.seleccionarMejorRepartidor(-33.45, -70.66))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay repartidores disponibles");
    }

    // -------------------------------------------------------------------------
    // obtenerMetricas()
    // -------------------------------------------------------------------------
    @Test
    void obtenerMetricas_deberiaCalcularTasaDeExitoCorrectamente() {
        Delivery entregado = Delivery.builder().estado(EstadoDelivery.ENTREGADO).build();
        Delivery fallido = Delivery.builder().estado(EstadoDelivery.FALLIDO).build();

        when(repartidorRepository.findById(1L)).thenReturn(Optional.of(repartidor));
        when(deliveryRepository.findByRepartidorIdOrderByFechaAsignacionDesc(1L))
                .thenReturn(List.of(entregado, entregado, entregado, entregado, fallido));

        MetricasRepartidorResponse resultado = repartidorService.obtenerMetricas(1L);

        assertThat(resultado.getTotalEntregas()).isEqualTo(5);
        assertThat(resultado.getEntregasExitosas()).isEqualTo(4);
        assertThat(resultado.getEntregasFallidas()).isEqualTo(1);
        assertThat(resultado.getTasaExito()).isEqualTo(80.0);
    }
}