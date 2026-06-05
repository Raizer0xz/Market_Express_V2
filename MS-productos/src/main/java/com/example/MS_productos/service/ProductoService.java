package com.example.MS_productos.service;

import com.example.MS_productos.model.Producto;
import com.example.MS_productos.repository.CategoriaRepository;
import com.example.MS_productos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        log.info("Listando todos los productos activos");
        return productoRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Long categoriaId) {
        categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + categoriaId));
        return productoRepository.findByCategoriaIdAndActivoTrue(categoriaId);
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre);
    }

    @Transactional
    public Producto crearProducto(Producto producto) {
        categoriaRepository.findById(producto.getCategoria().getId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        log.info("Creando producto: {}", producto.getNombre());
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizarProducto(Long id, Producto datos) {
        Producto existente = obtenerPorId(id);
        categoriaRepository.findById(datos.getCategoria().getId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setImagenUrl(datos.getImagenUrl());
        existente.setUnidadMedida(datos.getUnidadMedida());
        existente.setCategoria(datos.getCategoria());
        log.info("Actualizando producto: {}", id);
        return productoRepository.save(existente);
    }

    @Transactional
    public void desactivarProducto(Long id) {
        Producto p = obtenerPorId(id);
        p.setActivo(false);
        productoRepository.save(p);
        log.info("Producto {} desactivado", id);
    }
}