package com.example.MS_carrito.service;

import com.example.MS_carrito.client.ProductoClient;
import com.example.MS_carrito.dto.ItemCarritoDetalleDTO;
import com.example.MS_carrito.dto.ProductoDTO;
import com.example.MS_carrito.model.Carrito;
import com.example.MS_carrito.model.ItemCarrito;
import com.example.MS_carrito.repository.CarritoRepository;
import com.example.MS_carrito.repository.ItemCarritoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private ItemCarritoRepository itemCarritoRepository;
    @Mock private ProductoClient productoClient;

    @InjectMocks
    private CarritoService carritoService;

    private Carrito carrito;
    private ItemCarrito item;

    @BeforeEach
    void setUp() {
        carrito = Carrito.builder()
                .id(1L).usuarioId(10L).sucursalId(2L).estado("ACTIVO").build();

        item = ItemCarrito.builder()
                .id(1L).productoId(5L).cantidad(2)
                .precioUnitario(new BigDecimal("1500.00")).carrito(carrito).build();
    }

    // -------------------------------------------------------------------------
    // crearCarrito()
    // -------------------------------------------------------------------------
    @Test
    void crearCarrito_deberiaGuardarYRetornarCarrito() {
        when(carritoRepository.findByUsuarioIdAndEstado(10L, "ACTIVO")).thenReturn(Optional.empty());
        when(carritoRepository.save(any())).thenReturn(carrito);

        Carrito resultado = carritoService.crearCarrito(carrito);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsuarioId()).isEqualTo(10L);
        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
        verify(carritoRepository).save(carrito);
    }

    @Test
    void crearCarrito_deberiaLanzarExcepcionSiUsuarioYaTieneCarritoActivo() {
        when(carritoRepository.findByUsuarioIdAndEstado(10L, "ACTIVO"))
                .thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> carritoService.crearCarrito(carrito))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El usuario ya tiene un carrito activo");

        verify(carritoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // obtenerCarritoActivo()
    // -------------------------------------------------------------------------
    @Test
    void obtenerCarritoActivo_deberiaRetornarCarritoCuandoExiste() {
        when(carritoRepository.findByUsuarioIdAndEstado(10L, "ACTIVO"))
                .thenReturn(Optional.of(carrito));

        Carrito resultado = carritoService.obtenerCarritoActivo(10L);

        assertThat(resultado.getUsuarioId()).isEqualTo(10L);
        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
        verify(carritoRepository).findByUsuarioIdAndEstado(10L, "ACTIVO");
    }

    @Test
    void obtenerCarritoActivo_deberiaLanzarExcepcionCuandoNoExiste() {
        when(carritoRepository.findByUsuarioIdAndEstado(99L, "ACTIVO"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.obtenerCarritoActivo(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay carrito activo para el usuario: 99");
    }

    // -------------------------------------------------------------------------
    // agregarItem() — ítem nuevo
    // -------------------------------------------------------------------------
    @Test
    void agregarItem_deberiaCrearNuevoItemCuandoProductoNoEstaEnCarrito() {
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarritoIdAndProductoId(1L, 5L))
                .thenReturn(Optional.empty());
        when(itemCarritoRepository.save(any())).thenReturn(item);

        ItemCarrito resultado = carritoService.agregarItem(1L, item);

        assertThat(resultado.getProductoId()).isEqualTo(5L);
        assertThat(resultado.getCantidad()).isEqualTo(2);
        verify(itemCarritoRepository).save(item);
    }

    // -------------------------------------------------------------------------
    // agregarItem() — ítem existente suma cantidad
    // -------------------------------------------------------------------------
    @Test
    void agregarItem_deberiaSumarCantidadSiProductoYaEstaEnCarrito() {
        ItemCarrito existente = ItemCarrito.builder()
                .id(1L).productoId(5L).cantidad(3)
                .precioUnitario(new BigDecimal("1500.00")).carrito(carrito).build();

        ItemCarrito nuevo = ItemCarrito.builder()
                .productoId(5L).cantidad(2)
                .precioUnitario(new BigDecimal("1500.00")).build();

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarritoIdAndProductoId(1L, 5L))
                .thenReturn(Optional.of(existente));
        when(itemCarritoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ItemCarrito resultado = carritoService.agregarItem(1L, nuevo);

        assertThat(resultado.getCantidad()).isEqualTo(5); // 3 + 2
        verify(itemCarritoRepository).save(existente);
    }

    @Test
    void agregarItem_deberiaLanzarExcepcionSiCarritoNoExiste() {
        when(carritoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.agregarItem(99L, item))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Carrito no encontrado: 99");

        verify(itemCarritoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // calcularTotal()
    // -------------------------------------------------------------------------
    @Test
    void calcularTotal_deberiaRetornarSumaDeItems() {
        ItemCarrito item2 = ItemCarrito.builder()
                .id(2L).productoId(6L).cantidad(1)
                .precioUnitario(new BigDecimal("500.00")).carrito(carrito).build();

        when(itemCarritoRepository.findByCarritoId(1L)).thenReturn(List.of(item, item2));

        BigDecimal total = carritoService.calcularTotal(1L);

        // item: 1500 * 2 = 3000 + item2: 500 * 1 = 500 → total = 3500
        assertThat(total).isEqualByComparingTo("3500.00");
        verify(itemCarritoRepository).findByCarritoId(1L);
    }

    @Test
    void calcularTotal_deberiaRetornarCeroCuandoCarritoEstaVacio() {
        when(itemCarritoRepository.findByCarritoId(1L)).thenReturn(List.of());

        BigDecimal total = carritoService.calcularTotal(1L);

        assertThat(total).isEqualByComparingTo("0.00");
    }

    // -------------------------------------------------------------------------
    // eliminarItem()
    // -------------------------------------------------------------------------
    @Test
    void eliminarItem_deberiaEliminarCuandoExiste() {
        when(itemCarritoRepository.findById(1L)).thenReturn(Optional.of(item));
        doNothing().when(itemCarritoRepository).deleteById(1L);

        carritoService.eliminarItem(1L);

        verify(itemCarritoRepository).deleteById(1L);
    }

    @Test
    void eliminarItem_deberiaLanzarExcepcionCuandoNoExiste() {
        when(itemCarritoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.eliminarItem(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ítem no encontrado: 99");

        verify(itemCarritoRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // eliminarCarrito()
    // -------------------------------------------------------------------------
    @Test
    void eliminarCarrito_deberiaEliminarCuandoExiste() {
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        doNothing().when(carritoRepository).delete(carrito);

        carritoService.eliminarCarrito(1L);

        verify(carritoRepository).delete(carrito);
    }

    @Test
    void eliminarCarrito_deberiaLanzarExcepcionCuandoNoExiste() {
        when(carritoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.eliminarCarrito(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Carrito no encontrado: 99");

        verify(carritoRepository, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // confirmarCarrito()
    // -------------------------------------------------------------------------
    @Test
    void confirmarCarrito_deberiaCambiarEstadoAConfirmado() {
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Carrito resultado = carritoService.confirmarCarrito(1L);

        assertThat(resultado.getEstado()).isEqualTo("CONFIRMADO");
        verify(carritoRepository).save(carrito);
    }

    @Test
    void confirmarCarrito_deberiaLanzarExcepcionSiCarritoNoExiste() {
        when(carritoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.confirmarCarrito(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Carrito no encontrado: 99");
    }

    // -------------------------------------------------------------------------
    // listarItemsConDetalle()
    // -------------------------------------------------------------------------
    @Test
    void listarItemsConDetalle_deberiaRetornarDTOsConNombreDeProducto() {
        ProductoDTO productoDTO = new ProductoDTO();
        productoDTO.setNombre("Leche Entera");
        productoDTO.setUnidadMedida("litro");

        when(itemCarritoRepository.findByCarritoId(1L)).thenReturn(List.of(item));
        when(productoClient.obtenerProducto(5L)).thenReturn(productoDTO);

        List<ItemCarritoDetalleDTO> resultado = carritoService.listarItemsConDetalle(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo("Leche Entera");
        assertThat(resultado.get(0).getUnidadMedida()).isEqualTo("litro");
        assertThat(resultado.get(0).getSubtotal()).isEqualByComparingTo("3000.00");
    }

    @Test
    void listarItemsConDetalle_deberiaUsarNombreGenericoSiFeignFalla() {
        when(itemCarritoRepository.findByCarritoId(1L)).thenReturn(List.of(item));
        when(productoClient.obtenerProducto(5L))
                .thenThrow(new RuntimeException("MS-productos no disponible"));

        List<ItemCarritoDetalleDTO> resultado = carritoService.listarItemsConDetalle(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo("Producto #5");
    }
}