package com.example.MS_productos.service;

import com.example.MS_productos.model.PrecioProducto;
import com.example.MS_productos.model.Producto;
import com.example.MS_productos.repository.PrecioProductoRepository;
import com.example.MS_productos.repository.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PrecioProductoService {

    @Autowired
    private PrecioProductoRepository precioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // Listar todos los precios de un producto
    public List<PrecioProducto> listarPorProducto(Long productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productoId));
        return precioRepository.findByProductoId(productoId);
    }

    // Listar precios de un producto en una sucursal específica
    public List<PrecioProducto> listarPorProductoYSucursal(Long productoId, Long sucursalId) {
        return precioRepository.findByProductoIdAndSucursalId(productoId, sucursalId);
    }

    // Registrar un nuevo precio para un producto
    public PrecioProducto crearPrecio(PrecioProducto precio) {
        Producto producto = productoRepository.findById(precio.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        precio.setProducto(producto);
        log.info("Registrando precio para producto: {}", producto.getNombre());
        return precioRepository.save(precio);
    }

    // Eliminar un precio por ID
    public void eliminarPrecio(Long id) {
        precioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Precio no encontrado: " + id));
        precioRepository.deleteById(id);
        log.info("Precio {} eliminado", id);
    }
}