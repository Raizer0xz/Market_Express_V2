package com.example.MS_productos.service;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.model.PrecioProducto;
import com.example.MS_productos.model.Producto;
import com.example.MS_productos.repository.PrecioProductoRepository;
import com.example.MS_productos.repository.ProductoRepository;
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
class PrecioProductoServiceTest {

    @Mock
    private PrecioProductoRepository precioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private PrecioProductoService precioProductoService;

    private Producto producto;
    private PrecioProducto precio;

    @BeforeEach
    void setUp() {
        producto = Producto.builder()
                .id(1L)
                .nombre("Leche Entera")
                .categoria(Categoria.builder().id(1L).nombre("Lácteos").build())
                .activo(true)
                .build();

        precio = PrecioProducto.builder()
                .id(1L)
                .producto(producto)
                .sucursalId(2L)
                .precio(new BigDecimal("1500.00"))
                .build();
    }

    // -------------------------------------------------------------------------
    // listarPorProducto()
    // -------------------------------------------------------------------------
    @Test
    void listarPorProducto_deberiaRetornarPreciosDelProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(precioRepository.findByProductoId(1L)).thenReturn(List.of(precio));

        List<PrecioProducto> resultado = precioProductoService.listarPorProducto(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPrecio()).isEqualByComparingTo("1500.00");
        assertThat(resultado.get(0).getSucursalId()).isEqualTo(2L);
        verify(productoRepository).findById(1L);
        verify(precioRepository).findByProductoId(1L);
    }

    @Test
    void listarPorProducto_deberiaLanzarExcepcionCuandoProductoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> precioProductoService.listarPorProducto(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto no encontrado: 99");

        verify(productoRepository).findById(99L);
        verify(precioRepository, never()).findByProductoId(any());
    }

    // -------------------------------------------------------------------------
    // listarPorProductoYSucursal()
    // -------------------------------------------------------------------------
    @Test
    void listarPorProductoYSucursal_deberiaRetornarPreciosFiltrados() {
        when(precioRepository.findByProductoIdAndSucursalId(1L, 2L)).thenReturn(List.of(precio));

        List<PrecioProducto> resultado = precioProductoService.listarPorProductoYSucursal(1L, 2L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSucursalId()).isEqualTo(2L);
        verify(precioRepository).findByProductoIdAndSucursalId(1L, 2L);
    }

    @Test
    void listarPorProductoYSucursal_deberiaRetornarListaVaciaCuandoNoHayPrecios() {
        when(precioRepository.findByProductoIdAndSucursalId(1L, 99L)).thenReturn(List.of());

        List<PrecioProducto> resultado = precioProductoService.listarPorProductoYSucursal(1L, 99L);

        assertThat(resultado).isEmpty();
        verify(precioRepository).findByProductoIdAndSucursalId(1L, 99L);
    }

    // -------------------------------------------------------------------------
    // crearPrecio()
    // -------------------------------------------------------------------------
    @Test
    void crearPrecio_deberiaGuardarYRetornarPrecio() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(precioRepository.save(any(PrecioProducto.class))).thenReturn(precio);

        PrecioProducto resultado = precioProductoService.crearPrecio(precio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getPrecio()).isEqualByComparingTo("1500.00");
        assertThat(resultado.getSucursalId()).isEqualTo(2L);
        verify(productoRepository).findById(1L);
        verify(precioRepository).save(precio);
    }

    @Test
    void crearPrecio_deberiaLanzarExcepcionCuandoProductoNoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> precioProductoService.crearPrecio(precio))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto no encontrado");

        verify(productoRepository).findById(1L);
        verify(precioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // eliminarPrecio()
    // -------------------------------------------------------------------------
    @Test
    void eliminarPrecio_deberiaEliminarCuandoExiste() {
        when(precioRepository.findById(1L)).thenReturn(Optional.of(precio));
        doNothing().when(precioRepository).deleteById(1L);

        precioProductoService.eliminarPrecio(1L);

        verify(precioRepository).findById(1L);
        verify(precioRepository).deleteById(1L);
    }

    @Test
    void eliminarPrecio_deberiaLanzarExcepcionCuandoNoExiste() {
        when(precioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> precioProductoService.eliminarPrecio(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Precio no encontrado: 99");

        verify(precioRepository).findById(99L);
        verify(precioRepository, never()).deleteById(any());
    }
}