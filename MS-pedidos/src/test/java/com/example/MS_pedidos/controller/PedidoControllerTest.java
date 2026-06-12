
package com.example.MS_pedidos.controller;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;


    @Test
    void deberiaRetornarPedsidoPorId() throws Exception {

        Pedido paciente = new Pedido();
        paciente.setIdPedido(1L);
        paciente.setNombres("Juan");

        when(service.findById(1L))
                .thenReturn(Optional.of(pedido));

        mockMvc.perform(get("/api/v1/pedido/1")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(1))
                .andExpect(jsonPath("$.nombres").value("Juan"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists());

        verify(service).findById(1L);
    }

    @Test
    void deberiaCrearPedido() throws Exception {

        Pedido pedido = new Pedido();

        pedido.setId(1L);
        pedido.setUsuarioId(Long.valueOf(1L));
        pedido.setSucursalId(Long.valueOf(1L));
        pedido.setDireccionEntrega("Melipollo #621");
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedido.setTotal(new BigDecimal(1100000));
        pedido.setCarritoId(Long.valueOf(1L));
        when(service.save(any(Pedido.class)))
                .thenReturn(pedido);

        String json = """
                {
                        "id": 1,
                        "usuarioId": 1,
                        "sucursalId": 10,
                        "carritoId": 500,
                        "estado": "PENDIENTE",
                        "total": 1550.50,
                        "direccionEntrega": "Av. Siempreviva 742, Springfield",
                        "createdAt": "2026-06-12T13:31:46",
                        "updatedAt": "2026-06-12T13:31:46"
                    }
                """;

        mockMvc.perform(post("/api/v1/pedido")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPedido").value(Optional.of(1)))
                .andExpect(jsonPath("$.rut").value("12345678-9"))
                .andExpect(jsonPath("$.nombres").value("Juan"))
                .andExpect(jsonPath("$.apellidos").value("Perez"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(service).save(any(Pedido.class));
    }

    @Test
    void deberiaRetornar400CuandoFaltanCamposObligatorios() throws Exception {

        String json = """
                {
                    "nombres": "Juan"
                }
                """;

        mockMvc.perform(post("/api/v1/pedido")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isBadRequest());

    }
}