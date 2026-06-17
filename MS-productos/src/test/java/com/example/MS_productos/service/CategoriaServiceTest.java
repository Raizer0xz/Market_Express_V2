package com.example.MS_productos.service;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder()
                .id(1L)
                .nombre("Lácteos")
                .descripcion("Productos lácteos")
                .imagenUrl("https://img.example.com/lacteos.png")
                .build();
    }

    // -------------------------------------------------------------------------
    // listarCategorias()
    // -------------------------------------------------------------------------
    @Test
    void listarCategorias_deberiaRetornarTodasLasCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));

        List<Categoria> resultado = categoriaService.listarCategorias();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Lácteos");
        verify(categoriaRepository).findAll();
    }

    @Test
    void listarCategorias_deberiaRetornarListaVaciaCuandoNoHayCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of());

        List<Categoria> resultado = categoriaService.listarCategorias();

        assertThat(resultado).isEmpty();
        verify(categoriaRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // obtenerPorId()
    // -------------------------------------------------------------------------
    @Test
    void obtenerPorId_deberiaRetornarCategoriaCuandoExiste() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Categoria resultado = categoriaService.obtenerPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Lácteos");
        verify(categoriaRepository).findById(1L);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionCuandoNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada: 99");

        verify(categoriaRepository).findById(99L);
    }

    // -------------------------------------------------------------------------
    // crearCategoria()
    // -------------------------------------------------------------------------
    @Test
    void crearCategoria_deberiaGuardarYRetornarCategoria() {
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        Categoria resultado = categoriaService.crearCategoria(categoria);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo("Lácteos");
        verify(categoriaRepository).save(categoria);
    }

    // -------------------------------------------------------------------------
    // actualizarCategoria()
    // -------------------------------------------------------------------------
    @Test
    void actualizarCategoria_deberiaActualizarYRetornarCategoria() {
        Categoria datosNuevos = Categoria.builder()
                .id(1L)
                .nombre("Lácteos y Huevos")
                .descripcion("Lácteos y huevos frescos")
                .build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(datosNuevos);

        Categoria resultado = categoriaService.actualizarCategoria(1L, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Lácteos y Huevos");
        assertThat(resultado.getDescripcion()).isEqualTo("Lácteos y huevos frescos");
        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void actualizarCategoria_deberiaLanzarExcepcionCuandoCategoriaNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.actualizarCategoria(99L, categoria))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada: 99");

        verify(categoriaRepository).findById(99L);
        verify(categoriaRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // eliminarCategoria()
    // -------------------------------------------------------------------------
    @Test
    void eliminarCategoria_deberiaEliminarCuandoExisteYNoTieneProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        doNothing().when(categoriaRepository).deleteById(1L);

        categoriaService.eliminarCategoria(1L);

        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    void eliminarCategoria_deberiaLanzarExcepcionCuandoCategoriaNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.eliminarCategoria(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada: 99");

        verify(categoriaRepository).findById(99L);
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    void eliminarCategoria_deberiaLanzarExcepcionCuandoTieneProductosAsociados() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        doThrow(new RuntimeException("constraint violation"))
                .when(categoriaRepository).deleteById(1L);

        assertThatThrownBy(() -> categoriaService.eliminarCategoria(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede eliminar la categoría porque tiene productos asociados.");

        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).deleteById(1L);
    }
}