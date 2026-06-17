package com.example.MS_sucursales.service;

import com.example.MS_sucursales.model.Sucursal;
import com.example.MS_sucursales.repository.SucursalRepository;
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
class SucursalServiceTest {

    @Mock
    private SucursalRepository repository;

    @InjectMocks
    private SucursalService sucursalService;

    private Sucursal sucursal;

    @BeforeEach
    void setUp() {
        sucursal = Sucursal.builder()
                .id(1L)
                .nombre("Sucursal Centro")
                .direccion("Av. Libertador 123")
                .latitud(-33.4489)
                .longitud(-70.6693)
                .horarioApertura("08:00")
                .horarioCierre("22:00")
                .abierta(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // obtenerTodas()
    // -------------------------------------------------------------------------
    @Test
    void obtenerTodas_deberiaRetornarListaDeSucursales() {
        when(repository.findAll()).thenReturn(List.of(sucursal));

        List<Sucursal> resultado = sucursalService.obtenerTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Sucursal Centro");
        verify(repository).findAll();
    }

    @Test
    void obtenerTodas_deberiaRetornarListaVaciaCuandoNoHaySucursales() {
        when(repository.findAll()).thenReturn(List.of());

        List<Sucursal> resultado = sucursalService.obtenerTodas();

        assertThat(resultado).isEmpty();
        verify(repository).findAll();
    }

    // -------------------------------------------------------------------------
    // obtenerAbiertas()
    // -------------------------------------------------------------------------
    @Test
    void obtenerAbiertas_deberiaRetornarSoloLasAbiertas() {
        when(repository.findByAbiertaTrue()).thenReturn(List.of(sucursal));

        List<Sucursal> resultado = sucursalService.obtenerAbiertas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isAbierta()).isTrue();
        verify(repository).findByAbiertaTrue();
    }

    // -------------------------------------------------------------------------
    // obtenerPorId()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorId_deberiaRetornarSucursalCuandoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(sucursal));

        Sucursal resultado = sucursalService.obtenerPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Sucursal Centro");
        verify(repository).findById(1L);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionCuandoNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sucursalService.obtenerPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sucursal no encontrada con el ID: 99");

        verify(repository).findById(99L);
    }

    // -------------------------------------------------------------------------
    // guardar()
    // -------------------------------------------------------------------------
    @Test
    void guardar_deberiaGuardarYRetornarSucursal() {
        when(repository.save(any(Sucursal.class))).thenReturn(sucursal);

        Sucursal resultado = sucursalService.guardar(sucursal);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Sucursal Centro");
        verify(repository).save(sucursal);
    }

    // -------------------------------------------------------------------------
    // eliminar()
    // -------------------------------------------------------------------------
    @Test
    void eliminar_deberiaEliminarCuandoSucursalExiste() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        sucursalService.eliminar(1L);

        verify(repository).existsById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void eliminar_deberiaLanzarExcepcionCuandoSucursalNoExiste() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sucursalService.eliminar(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede eliminar: Sucursal no existe");

        verify(repository).existsById(99L);
        verify(repository, never()).deleteById(any());
    }
}