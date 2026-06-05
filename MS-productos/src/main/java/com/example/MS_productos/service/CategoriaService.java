package com.example.MS_productos.service;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        log.info("Listando todas las categorías");
        return categoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Categoria obtenerPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));
    }

    @Transactional
    public Categoria crearCategoria(Categoria categoria) {
        log.info("Creando categoría: {}", categoria.getNombre());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria actualizarCategoria(Long id, Categoria datos) {
        Categoria existente = obtenerPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setImagenUrl(datos.getImagenUrl());
        log.info("Actualizando categoría: {}", id);
        return categoriaRepository.save(existente);
    }

    @Transactional
    public void eliminarCategoria(Long id) {
        obtenerPorId(id);
        try {
            categoriaRepository.deleteById(id);
            log.info("Categoría {} eliminada", id);
        } catch (Exception e) {
            throw new RuntimeException(
                    "No se puede eliminar la categoría porque tiene productos asociados.");
        }
    }
}