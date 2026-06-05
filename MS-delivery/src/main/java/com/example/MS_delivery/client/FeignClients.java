package com.example.MS_delivery.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Clientes Feign para comunicación inter-microservicio.
 * Los nombres ('MS-PEDIDOS', 'MS-USUARIOS') deben coincidir
 * con spring.application.name de cada servicio en Eureka.
 */
public class FeignClients {

    // ────────────────────────────────────────────────────────────────────────
    // MS-PEDIDOS
    // ────────────────────────────────────────────────────────────────────────

    @FeignClient(name = "MS-PEDIDOS")
    public interface PedidoClient {

        /**
         * Valida que el pedido existe y obtiene la dirección destino.
         * Adaptar la ruta a la que use MS-pedidos en tu proyecto.
         */
        @GetMapping("/api/v1/pedidos/{id}")
        PedidoResponse obtenerPedido(@PathVariable("id") Long id);
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PedidoResponse {
        private Long id;
        private String estado;
        private String direccionEntrega;
        private Double latitudEntrega;
        private Double longitudEntrega;
        // Agrega los campos que tenga tu modelo de pedido
    }

    // ────────────────────────────────────────────────────────────────────────
    // MS-USUARIOS
    // ────────────────────────────────────────────────────────────────────────

    @FeignClient(name = "MS-USUARIOS")
    public interface UsuarioClient {

        @GetMapping("/api/v1/usuarios/{id}")
        UsuarioResponse obtenerUsuario(@PathVariable("id") Long id);
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioResponse {
        private Long id;
        private String nombre;
        private String email;
        private String telefono;
    }
}
