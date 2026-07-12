package com.example.MS_inventario.service;

import com.example.MS_inventario.model.Inventario;
import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import com.example.MS_inventario.repository.InventarioRepository;
import com.example.MS_inventario.repository.MovimientoInventarioRepository;
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
class InventarioServiceTest {

    @Mock private InventarioRepository inventarioRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @InjectMocks private InventarioService service;

    private Inventario inventario;

    @BeforeEach
    void setUp() {
        inventario = Inventario.builder()
                .id(1L).productoId(10L).sucursalId(2L)
                .cantidad(50).stockMinimo(5).build();
    }

    // verStock()
    @Test
    void verStock_deberiaRetornarInventarioCuandoExiste() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        assertThat(service.verStock(10L, 2L).getCantidad()).isEqualTo(50);
    }

    @Test
    void verStock_deberiaLanzarExcepcionCuandoNoExiste() {
        when(inventarioRepository.findByProductoIdAndSucursalId(99L, 99L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verStock(99L, 99L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("No hay inventario");
    }

    // aumentarStock()
    @Test
    void aumentarStock_deberiaIncrementarCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any())).thenReturn(inventario);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        assertThat(service.aumentarStock(10L, 2L, 20, "Recepcion", 1L, "ADMIN").getCantidad())
                .isEqualTo(70);
    }

    @Test
    void aumentarStock_deberiaLanzarExcepcionSiCantidadEsCero() {
        assertThatThrownBy(() -> service.aumentarStock(10L, 2L, 0, "motivo", 1L, "ADMIN"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("mayor a 0");
        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void aumentarStock_deberiaLanzarExcepcionSiNoEsAdmin() {
        assertThatThrownBy(() -> service.aumentarStock(10L, 2L, 10, "motivo", 1L, "CLIENTE"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("ADMIN");
        verify(inventarioRepository, never()).save(any());
    }

    // reducirStock()
    @Test
    void reducirStock_deberiaDecrementarCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any())).thenReturn(inventario);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        assertThat(service.reducirStock(10L, 2L, 10, "Pedido", 1L, "ADMIN").getCantidad())
                .isEqualTo(40);
    }

    @Test
    void reducirStock_deberiaLanzarExcepcionSiStockInsuficiente() {
        inventario.setCantidad(5);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        assertThatThrownBy(() -> service.reducirStock(10L, 2L, 10, "Pedido", 1L, "ADMIN"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Stock insuficiente");
    }

    @Test
    void reducirStock_deberiaLanzarExcepcionSiNoEsAdmin() {
        assertThatThrownBy(() -> service.reducirStock(10L, 2L, 10, "motivo", 1L, "CLIENTE"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("ADMIN");
    }

    // ajustarStock()
    @Test
    void ajustarStock_deberiaEstablecerNuevaCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any())).thenReturn(inventario);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        assertThat(service.ajustarStock(10L, 2L, 35, "Conteo", 1L, "ADMIN").getCantidad())
                .isEqualTo(35);
    }

    @Test
    void ajustarStock_deberiaLanzarExcepcionSiCantidadNegativa() {
        assertThatThrownBy(() -> service.ajustarStock(10L, 2L, -1, "motivo", 1L, "ADMIN"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("negativa");
    }

    @Test
    void ajustarStock_deberiaLanzarExcepcionSiNoEsAdmin() {
        assertThatThrownBy(() -> service.ajustarStock(10L, 2L, 35, "motivo", 1L, "CLIENTE"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("ADMIN");
    }

    // obtenerAlertasGlobales()
    @Test
    void obtenerAlertasGlobales_deberiaRetornarProductosConStockBajo() {
        inventario.setCantidad(3);
        when(inventarioRepository.findStockBajo()).thenReturn(List.of(inventario));
        assertThat(service.obtenerAlertasGlobales()).hasSize(1);
    }

    // actualizarStockMinimo()
    @Test
    void actualizarStockMinimo_deberiaActualizarUmbral() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any())).thenReturn(inventario);

        assertThat(service.actualizarStockMinimo(10L, 2L, 10, "ADMIN").getStockMinimo())
                .isEqualTo(10);
    }

    @Test
    void actualizarStockMinimo_deberiaLanzarExcepcionSiMinimoNegativo() {
        assertThatThrownBy(() -> service.actualizarStockMinimo(10L, 2L, -1, "ADMIN"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("negativo");
    }

    @Test
    void actualizarStockMinimo_deberiaLanzarExcepcionSiNoEsAdmin() {
        assertThatThrownBy(() -> service.actualizarStockMinimo(10L, 2L, 10, "CLIENTE"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("ADMIN");
    }

    // historialPorSucursal()
    @Test
    void historialPorSucursal_deberiaRetornarHistorialCuandoEsAdmin() {
        when(movimientoRepository.findBySucursalIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(MovimientoInventario.builder().build()));
        assertThat(service.historialPorSucursal(2L, "ADMIN")).hasSize(1);
    }

    @Test
    void historialPorSucursal_deberiaLanzarExcepcionSiNoEsAdmin() {
        assertThatThrownBy(() -> service.historialPorSucursal(2L, "CLIENTE"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("ADMIN");
    }
}