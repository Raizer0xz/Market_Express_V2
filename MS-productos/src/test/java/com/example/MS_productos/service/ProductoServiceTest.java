package com.example.MS_productos.service;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.model.Producto;
import com.example.MS_productos.repository.CategoriaRepository;
import com.example.MS_productos.repository.ProductoRepository;
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
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;

    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder()
                .id(1L)
                .nombre("Lácteos")
                .descripcion("Productos lácteos")
                .build();

        producto = Producto.builder()
                .id(1L)
                .nombre("Leche Entera")
                .descripcion("Leche entera 1L")
                .unidadMedida("litro")
                .activo(true)
                .categoria(categoria)
                .build();
    }

    // -------------------------------------------------------------------------
    // listarProductos()
    // -------------------------------------------------------------------------
    @Test
    void listarProductos_deberiaRetornarProductosActivos() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.listarProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Leche Entera");
        assertThat(resultado.get(0).getActivo()).isTrue();
        verify(productoRepository).findByActivoTrue();
    }

    @Test
    void listarProductos_deberiaRetornarListaVaciaCuandoNoHayActivos() {
        when(productoRepository.findByActivoTrue()).thenReturn(List.of());

        List<Producto> resultado = productoService.listarProductos();

        assertThat(resultado).isEmpty();
        verify(productoRepository).findByActivoTrue();
    }

    // -------------------------------------------------------------------------
    // obtenerPorId()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorId_deberiaRetornarProductoCuandoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Producto resultado = productoService.obtenerPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Leche Entera");
        verify(productoRepository).findById(1L);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionCuandoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto no encontrado: 99");

        verify(productoRepository).findById(99L);
    }

    // -------------------------------------------------------------------------
    // listarPorCategoria()
    // -------------------------------------------------------------------------
    @Test
    void listarPorCategoria_deberiaRetornarProductosDeLaCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.findByCategoriaIdAndActivoTrue(1L)).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.listarPorCategoria(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCategoria().getId()).isEqualTo(1L);
        verify(categoriaRepository).findById(1L);
        verify(productoRepository).findByCategoriaIdAndActivoTrue(1L);
    }

    @Test
    void listarPorCategoria_deberiaLanzarExcepcionCuandoCategoriaNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.listarPorCategoria(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada: 99");

        verify(categoriaRepository).findById(99L);
        verify(productoRepository, never()).findByCategoriaIdAndActivoTrue(any());
    }

    // -------------------------------------------------------------------------
    // buscarPorNombre()
    // -------------------------------------------------------------------------
    @Test
    void buscarPorNombre_deberiaRetornarProductosQueCoinciden() {
        when(productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue("leche"))
                .thenReturn(List.of(producto));

        List<Producto> resultado = productoService.buscarPorNombre("leche");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).containsIgnoringCase("leche");
        verify(productoRepository).findByNombreContainingIgnoreCaseAndActivoTrue("leche");
    }

    // -------------------------------------------------------------------------
    // crearProducto()
    // -------------------------------------------------------------------------
    @Test
    void crearProducto_deberiaGuardarYRetornarProducto() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        Producto resultado = productoService.crearProducto(producto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo("Leche Entera");
        verify(categoriaRepository).findById(1L);
        verify(productoRepository).save(producto);
    }

    @Test
    void crearProducto_deberiaLanzarExcepcionCuandoCategoriaNoExiste() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crearProducto(producto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada");

        verify(categoriaRepository).findById(1L);
        verify(productoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // actualizarProducto()
    // -------------------------------------------------------------------------
    @Test
    void actualizarProducto_deberiaActualizarYRetornarProducto() {
        Producto datosNuevos = Producto.builder()
                .id(1L)
                .nombre("Leche Descremada")
                .descripcion("Leche descremada 1L")
                .unidadMedida("litro")
                .categoria(categoria)
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenReturn(datosNuevos);

        Producto resultado = productoService.actualizarProducto(1L, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Leche Descremada");
        verify(productoRepository).findById(1L);
        verify(categoriaRepository).findById(1L);
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void actualizarProducto_deberiaLanzarExcepcionCuandoProductoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizarProducto(99L, producto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto no encontrado: 99");

        verify(productoRepository).findById(99L);
        verify(productoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // desactivarProducto()
    // -------------------------------------------------------------------------
    @Test
    void desactivarProducto_deberiaPonerActivoEnFalse() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        productoService.desactivarProducto(1L);

        assertThat(producto.getActivo()).isFalse();
        verify(productoRepository).findById(1L);
        verify(productoRepository).save(producto);
    }

    @Test
    void desactivarProducto_deberiaLanzarExcepcionCuandoProductoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.desactivarProducto(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto no encontrado: 99");

        verify(productoRepository).findById(99L);
        verify(productoRepository, never()).save(any());
    }
}