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

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private InventarioService service;

    private Inventario inventario;

    @BeforeEach
    void setUp() {
        inventario = Inventario.builder()
                .id(1L)
                .productoId(10L)
                .sucursalId(2L)
                .cantidad(50)
                .stockMinimo(5)
                .build();
    }

    // -------------------------------------------------------------------------
    // verStock()
    // -------------------------------------------------------------------------
    @Test
    void verStock_deberiaRetornarInventarioCuandoExiste() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));

        Inventario resultado = service.verStock(10L, 2L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCantidad()).isEqualTo(50);
        verify(inventarioRepository).findByProductoIdAndSucursalId(10L, 2L);
    }

    @Test
    void verStock_deberiaLanzarExcepcionCuandoNoExiste() {
        when(inventarioRepository.findByProductoIdAndSucursalId(99L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verStock(99L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay inventario");

        verify(inventarioRepository).findByProductoIdAndSucursalId(99L, 99L);
    }

    // -------------------------------------------------------------------------
    // verStockPorSucursal()
    // -------------------------------------------------------------------------
    @Test
    void verStockPorSucursal_deberiaRetornarListaDeInventario() {
        when(inventarioRepository.findBySucursalId(2L)).thenReturn(List.of(inventario));

        List<Inventario> resultado = service.verStockPorSucursal(2L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSucursalId()).isEqualTo(2L);
        verify(inventarioRepository).findBySucursalId(2L);
    }

    // -------------------------------------------------------------------------
    // aumentarStock()
    // -------------------------------------------------------------------------
    @Test
    void aumentarStock_deberiaIncrementarCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenReturn(MovimientoInventario.builder().build());

        Inventario resultado = service.aumentarStock(10L, 2L, 20, "Recepcion proveedor", 1L);

        assertThat(resultado.getCantidad()).isEqualTo(70);
        verify(inventarioRepository).save(inventario);
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void aumentarStock_deberiaCrearInventarioSiNoExiste() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.empty());
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        Inventario resultado = service.aumentarStock(10L, 2L, 30, "Primer ingreso", 1L);

        assertThat(resultado.getCantidad()).isEqualTo(30);
        verify(inventarioRepository).save(any(Inventario.class));
    }

    @Test
    void aumentarStock_deberiaLanzarExcepcionSiCantidadEsCero() {
        assertThatThrownBy(() -> service.aumentarStock(10L, 2L, 0, "motivo", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor a 0");

        verify(inventarioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // reducirStock()
    // -------------------------------------------------------------------------
    @Test
    void reducirStock_deberiaDecrementarCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        Inventario resultado = service.reducirStock(10L, 2L, 10, "Pedido #42", 1L);

        assertThat(resultado.getCantidad()).isEqualTo(40);
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void reducirStock_deberiaLanzarExcepcionSiStockInsuficiente() {
        inventario.setCantidad(5);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> service.reducirStock(10L, 2L, 10, "Pedido", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void reducirStock_deberiaLanzarExcepcionSiNoExisteInventario() {
        when(inventarioRepository.findByProductoIdAndSucursalId(99L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reducirStock(99L, 99L, 5, "Pedido", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay inventario");
    }

    // -------------------------------------------------------------------------
    // ajustarStock()
    // -------------------------------------------------------------------------
    @Test
    void ajustarStock_deberiaEstablecerNuevaCantidad() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().build());

        Inventario resultado = service.ajustarStock(10L, 2L, 35, "Conteo fisico", 1L);

        assertThat(resultado.getCantidad()).isEqualTo(35);
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void ajustarStock_deberiaLanzarExcepcionSiCantidadNegativa() {
        assertThatThrownBy(() -> service.ajustarStock(10L, 2L, -1, "motivo", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("negativa");
    }

    // -------------------------------------------------------------------------
    // obtenerAlertasGlobales()
    // -------------------------------------------------------------------------
    @Test
    void obtenerAlertasGlobales_deberiaRetornarProductosConStockBajo() {
        inventario.setCantidad(3);
        inventario.setStockMinimo(5);
        when(inventarioRepository.findStockBajo()).thenReturn(List.of(inventario));

        List<Inventario> resultado = service.obtenerAlertasGlobales();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCantidad()).isLessThanOrEqualTo(resultado.get(0).getStockMinimo());
        verify(inventarioRepository).findStockBajo();
    }

    // -------------------------------------------------------------------------
    // actualizarStockMinimo()
    // -------------------------------------------------------------------------
    @Test
    void actualizarStockMinimo_deberiaActualizarUmbral() {
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 2L))
                .thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);

        Inventario resultado = service.actualizarStockMinimo(10L, 2L, 10);

        assertThat(resultado.getStockMinimo()).isEqualTo(10);
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void actualizarStockMinimo_deberiaLanzarExcepcionSiMinimoNegativo() {
        assertThatThrownBy(() -> service.actualizarStockMinimo(10L, 2L, -1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("negativo");
    }
}