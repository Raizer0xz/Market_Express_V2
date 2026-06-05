package com.example.MS_carrito.client;

import com.example.MS_carrito.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "MS-PRODUCTO")
public interface ProductoClient {

    @GetMapping("/api/v2/productos/{id}")
    ProductoDTO obtenerProducto(@PathVariable Long id);
}