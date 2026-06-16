package com.example.MS_usuarios.controller;

import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuariosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;

    @Test
    void deberiaRetornarTodosLosPedidos() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(5L);
        pedido.setSucursalId(2L);
        pedido.setDireccionEntrega("Av. Siempreviva 742");
        pedido.setTotal(new BigDecimal("1550.50"));
        pedido.setEstado(EstadoPedido.PENDIENTE);

        when(service.findAll()).thenReturn(List.of(pedido));

        // El controller devuelve CollectionModel (HAL JSON),
        // los pedidos van dentro de "_embedded.pedidoList"
        mockMvc.perform(get("/api/v1/pedidos")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pedidoList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.pedidoList[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$._embedded.pedidoList[0]._links.todos.href").exists())
                .andExpect(jsonPath("$._embedded.pedidoList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.pedidoList[0]._links.delete.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(service).findAll();
    }

    @Test
    void deberiaCrearPedido() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(1L);
        pedido.setSucursalId(10L);
        pedido.setCarritoId(500L);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("1550.50"));
        pedido.setDireccionEntrega("Av. Siempreviva 742, Springfield");

        when(service.save(any(Pedido.class))).thenReturn(pedido);

        String json = """
                {
                    "usuarioId": 1,
                    "sucursalId": 10,
                    "carritoId": 500,
                    "estado": "PENDIENTE",
                    "total": 1550.50,
                    "direccionEntrega": "Av. Siempreviva 742, Springfield"
                }
                """;

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.direccionEntrega").value("Av. Siempreviva 742, Springfield"))
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).save(any(Pedido.class));
    }

    @Test
    void deberiaRetornar400CuandoFaltanCamposObligatorios() throws Exception {
        String json = """
                {
                    "usuarioId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deberiaActualizarEstadoDePedido() throws Exception {
        Pedido pedidoActualizado = new Pedido();
        pedidoActualizado.setId(1L);
        pedidoActualizado.setUsuarioId(1L);
        pedidoActualizado.setSucursalId(10L);
        pedidoActualizado.setEstado(EstadoPedido.CONFIRMADO);
        pedidoActualizado.setTotal(new BigDecimal("1550.50"));
        pedidoActualizado.setDireccionEntrega("Av. Siempreviva 742");

        when(service.update(eq(1L), eq(EstadoPedido.CONFIRMADO)))
                .thenReturn(Optional.of(pedidoActualizado));

        mockMvc.perform(put("/api/v1/pedidos/1/estado")
                        .param("nuevoEstado", "CONFIRMADO")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).update(1L, EstadoPedido.CONFIRMADO);
    }
}