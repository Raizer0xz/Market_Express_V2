package com.example.MS_carrito.service;

import com.example.MS_carrito.client.ProductoClient;
import com.example.MS_carrito.dto.ItemCarritoDetalleDTO;
import com.example.MS_carrito.dto.ProductoDTO;
import com.example.MS_carrito.model.Carrito;
import com.example.MS_carrito.model.ItemCarrito;
import com.example.MS_carrito.repository.CarritoRepository;
import com.example.MS_carrito.repository.ItemCarritoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private ProductoClient productoClient; // ✅ cliente Feign inyectado

    // ── métodos anteriores se mantienen igual ──

    public Carrito obtenerCarritoActivo(Long usuarioId) {
        return carritoRepository.findByUsuarioIdAndEstado(usuarioId, "ACTIVO")
                .orElseThrow(() -> new RuntimeException("No hay carrito activo para el usuario: " + usuarioId));
    }

    public Carrito crearCarrito(Carrito carrito) {
        carritoRepository.findByUsuarioIdAndEstado(carrito.getUsuarioId(), "ACTIVO")
                .ifPresent(c -> { throw new RuntimeException("El usuario ya tiene un carrito activo"); });
        log.info("Creando carrito para usuario: {}", carrito.getUsuarioId());
        return carritoRepository.save(carrito);
    }

    public ItemCarrito agregarItem(Long carritoId, ItemCarrito item) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + carritoId));
        item.setCarrito(carrito);
        return itemCarritoRepository
                .findByCarritoIdAndProductoId(carritoId, item.getProductoId())
                .map(existente -> {
                    existente.setCantidad(existente.getCantidad() + item.getCantidad());
                    return itemCarritoRepository.save(existente);
                })
                .orElseGet(() -> itemCarritoRepository.save(item));
    }

    public BigDecimal calcularTotal(Long carritoId) {
        List<ItemCarrito> items = itemCarritoRepository.findByCarritoId(carritoId);
        return items.stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void eliminarItem(Long itemId) {
        itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado: " + itemId));
        itemCarritoRepository.deleteById(itemId);
    }

    public Carrito confirmarCarrito(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + carritoId));
        carrito.setEstado("CONFIRMADO");
        return carritoRepository.save(carrito);
    }

    public List<ItemCarrito> listarItems(Long carritoId) {
        return itemCarritoRepository.findByCarritoId(carritoId);
    }

    public void eliminarCarrito(Long id) {
        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));
        carritoRepository.delete(carrito);
    }

    // ── MÉTODO NUEVO ── consulta nombre del producto a ms-productos
    public List<ItemCarritoDetalleDTO> listarItemsConDetalle(Long carritoId) {
        List<ItemCarrito> items = itemCarritoRepository.findByCarritoId(carritoId);

        return items.stream().map(item -> {
            String nombre = "Producto #" + item.getProductoId();
            String unidad = "-";

            try {
                ProductoDTO producto = productoClient.obtenerProducto(item.getProductoId());
                if (producto != null) {
                    nombre = producto.getNombre();
                    unidad = producto.getUnidadMedida();
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener nombre del producto {}: {}",
                        item.getProductoId(), e.getMessage());
            }

            BigDecimal subtotal = item.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));

            return new ItemCarritoDetalleDTO(
                    item.getId(),
                    item.getProductoId(),
                    nombre,
                    unidad,
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    subtotal
            );
        }).toList();
    }
}