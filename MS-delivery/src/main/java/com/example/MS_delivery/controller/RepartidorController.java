package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.UbicacionResponse;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    private Repartidor repartidorEjemplo() {
        return Repartidor.builder()
                .id(3L)
                .nombre("Carlos Perez")
                .telefono("987654321")
                .email("carlos@mail.com")
                .vehiculo(Repartidor.Vehiculo.MOTO)
                .estado(Repartidor.EstadoRepartidor.OCUPADO)
                .build();
    }

    private Delivery deliveryEjemplo() {
        return Delivery.builder()
                .id(1L)
                .pedidoId(42L)
                .repartidor(repartidorEjemplo())
                .direccionDestino("Av. Siempre Viva 123")
                .latitudDestino(-33.4489)
                .longitudDestino(-70.6693)
                .estado(EstadoDelivery.PENDIENTE)
                .intentos(0)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/delivery/asignar
    // -------------------------------------------------------------------------
    @Test
    void deberiaAsignarDeliveryAutomaticamente() throws Exception {
        when(deliveryService.asignarAutomaticamente(any())).thenReturn(deliveryEjemplo());

        String json = """
                {
                    "pedidoId": 42,
                    "direccionDestino": "Av. Siempre Viva 123",
                    "latitudDestino": -33.4489,
                    "longitudDestino": -70.6693
                }
                """;

        mockMvc.perform(post("/api/v1/delivery/asignar")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").value(42))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.ruta.href").exists())
                .andExpect(jsonPath("$._links.pedido.href").exists());

        verify(deliveryService).asignarAutomaticamente(any());
    }

    @Test
    void deberiaRetornar400AlAsignarSinRepartidoresDisponibles() throws Exception {
        when(deliveryService.asignarAutomaticamente(any()))
                .thenThrow(new RuntimeException("No hay repartidores disponibles en un radio de 10.0 km"));

        String json = """
                {
                    "pedidoId": 42,
                    "direccionDestino": "Av. Siempre Viva 123",
                    "latitudDestino": -33.4489,
                    "longitudDestino": -70.6693
                }
                """;

        mockMvc.perform(post("/api/v1/delivery/asignar")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No hay repartidores disponibles en un radio de 10.0 km"));
    }

    @Test
    void deberiaRetornar400AlAsignarConDatosInvalidos() throws Exception {
        String json = """
                {
                    "direccionDestino": "Sin pedidoId"
                }
                """;

        mockMvc.perform(post("/api/v1/delivery/asignar")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(deliveryService, never()).asignarAutomaticamente(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/{id}
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerDeliveryPorId() throws Exception {
        when(deliveryService.obtenerPorId(1L)).thenReturn(deliveryEjemplo());

        mockMvc.perform(get("/api/v1/delivery/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.direccionDestino").value("Av. Siempre Viva 123"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.ruta.href").exists())
                .andExpect(jsonPath("$._links.pedido.href").exists())
                .andExpect(jsonPath("$._links.historial-repartidor.href").exists());

        verify(deliveryService).obtenerPorId(1L);
    }

    @Test
    void deberiaRetornar404CuandoDeliveryNoExiste() throws Exception {
        when(deliveryService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Delivery no encontrado: 99"));

        mockMvc.perform(get("/api/v1/delivery/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Delivery no encontrado: 99"));

        verify(deliveryService).obtenerPorId(99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/pedido/{pedidoId}
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerDeliveryPorPedido() throws Exception {
        when(deliveryService.obtenerPorPedidoId(42L)).thenReturn(deliveryEjemplo());

        mockMvc.perform(get("/api/v1/delivery/pedido/42").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(42))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).obtenerPorPedidoId(42L);
    }

    @Test
    void deberiaRetornar404CuandoNoHayDeliveryParaPedido() throws Exception {
        when(deliveryService.obtenerPorPedidoId(999L))
                .thenThrow(new RuntimeException("Delivery no encontrado para pedido: 999"));

        mockMvc.perform(get("/api/v1/delivery/pedido/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Delivery no encontrado para pedido: 999"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/estado/{estado}
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarDeliveriesPorEstado() throws Exception {
        when(deliveryService.listarPorEstado(EstadoDelivery.EN_RUTA))
                .thenReturn(List.of(deliveryEjemplo()));

        mockMvc.perform(get("/api/v1/delivery/estado/EN_RUTA").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.deliveryList[0].pedidoId").value(42))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).listarPorEstado(EstadoDelivery.EN_RUTA);
    }

    @Test
    void deberiaRetornar400ConEstadoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/estado/NO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Estado inválido: NO_EXISTE"));

        verify(deliveryService, never()).listarPorEstado(any());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/delivery/{id}/iniciar-ruta
    // -------------------------------------------------------------------------
    @Test
    void deberiaIniciarRuta() throws Exception {
        Delivery enRuta = deliveryEjemplo();
        enRuta.setEstado(EstadoDelivery.EN_RUTA);
        when(deliveryService.iniciarRuta(1L)).thenReturn(enRuta);

        mockMvc.perform(put("/api/v1/delivery/1/iniciar-ruta").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_RUTA"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).iniciarRuta(1L);
    }

    @Test
    void deberiaRetornar400AlIniciarRutaConEstadoInvalido() throws Exception {
        when(deliveryService.iniciarRuta(1L))
                .thenThrow(new RuntimeException("No se puede 'iniciar ruta' con estado ENTREGADO. Se requiere: PENDIENTE"));

        mockMvc.perform(put("/api/v1/delivery/1/iniciar-ruta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "No se puede 'iniciar ruta' con estado ENTREGADO. Se requiere: PENDIENTE"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/delivery/{id}/entregar
    // -------------------------------------------------------------------------
    @Test
    void deberiaConfirmarEntrega() throws Exception {
        Delivery entregado = deliveryEjemplo();
        entregado.setEstado(EstadoDelivery.ENTREGADO);
        when(deliveryService.confirmarEntrega(1L)).thenReturn(entregado);

        mockMvc.perform(put("/api/v1/delivery/1/entregar").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).confirmarEntrega(1L);
    }

    @Test
    void deberiaRetornar400AlConfirmarEntregaConEstadoInvalido() throws Exception {
        when(deliveryService.confirmarEntrega(1L))
                .thenThrow(new RuntimeException("No se puede 'confirmar entrega' con estado PENDIENTE. Se requiere: EN_RUTA"));

        mockMvc.perform(put("/api/v1/delivery/1/entregar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "No se puede 'confirmar entrega' con estado PENDIENTE. Se requiere: EN_RUTA"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/delivery/{id}/fallo
    // -------------------------------------------------------------------------
    @Test
    void deberiaReportarFallo() throws Exception {
        Delivery fallido = deliveryEjemplo();
        fallido.setEstado(EstadoDelivery.FALLIDO);
        fallido.setNotas("Cliente no se encontraba");
        fallido.setIntentos(1);
        when(deliveryService.reportarFallo(1L, "Cliente no se encontraba")).thenReturn(fallido);

        String json = """
                { "notas": "Cliente no se encontraba" }
                """;

        mockMvc.perform(put("/api/v1/delivery/1/fallo")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDO"))
                .andExpect(jsonPath("$.notas").value("Cliente no se encontraba"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).reportarFallo(1L, "Cliente no se encontraba");
    }

    @Test
    void deberiaReportarFalloSinBody() throws Exception {
        Delivery fallido = deliveryEjemplo();
        fallido.setEstado(EstadoDelivery.FALLIDO);
        when(deliveryService.reportarFallo(1L, null)).thenReturn(fallido);

        mockMvc.perform(put("/api/v1/delivery/1/fallo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDO"));

        verify(deliveryService).reportarFallo(1L, null);
    }

    @Test
    void deberiaRetornar400AlReportarFalloEnEstadoFinal() throws Exception {
        when(deliveryService.reportarFallo(1L, null))
                .thenThrow(new RuntimeException("No se puede reportar fallo en estado ENTREGADO"));

        mockMvc.perform(put("/api/v1/delivery/1/fallo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede reportar fallo en estado ENTREGADO"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/delivery/{id}/cancelar
    // -------------------------------------------------------------------------
    @Test
    void deberiaCancelarDelivery() throws Exception {
        Delivery cancelado = deliveryEjemplo();
        cancelado.setEstado(EstadoDelivery.CANCELADO);
        cancelado.setNotas("Cliente cancelo el pedido");
        when(deliveryService.cancelar(1L, "Cliente cancelo el pedido")).thenReturn(cancelado);

        String json = """
                { "notas": "Cliente cancelo el pedido" }
                """;

        mockMvc.perform(put("/api/v1/delivery/1/cancelar")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(deliveryService).cancelar(1L, "Cliente cancelo el pedido");
    }

    @Test
    void deberiaRetornar400AlCancelarDeliveryYaEntregado() throws Exception {
        when(deliveryService.cancelar(1L, null))
                .thenThrow(new RuntimeException("No se puede cancelar un delivery ya entregado"));

        mockMvc.perform(put("/api/v1/delivery/1/cancelar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede cancelar un delivery ya entregado"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/pedido/{pedidoId}/ubicacion
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerUbicacionPorPedido() throws Exception {
        UbicacionResponse ubicacion = UbicacionResponse.builder()
                .repartidorId(3L)
                .nombreRepartidor("Carlos Perez")
                .latitud(-33.45)
                .longitud(-70.66)
                .estadoDelivery(EstadoDelivery.EN_RUTA)
                .etaMinutos(8)
                .build();
        when(deliveryService.obtenerUbicacionPorPedido(42L)).thenReturn(ubicacion);

        mockMvc.perform(get("/api/v1/delivery/pedido/42/ubicacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidorId").value(3))
                .andExpect(jsonPath("$.nombreRepartidor").value("Carlos Perez"))
                .andExpect(jsonPath("$.estadoDelivery").value("EN_RUTA"))
                .andExpect(jsonPath("$.etaMinutos").value(8));

        verify(deliveryService).obtenerUbicacionPorPedido(42L);
    }

    @Test
    void deberiaRetornar404CuandoNoHayUbicacionParaPedido() throws Exception {
        when(deliveryService.obtenerUbicacionPorPedido(999L))
                .thenThrow(new RuntimeException("No hay delivery activo para el pedido 999"));

        mockMvc.perform(get("/api/v1/delivery/pedido/999/ubicacion"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No hay delivery activo para el pedido 999"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/{id}/ruta
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerRutaDelivery() throws Exception {
        UbicacionHistorial punto = UbicacionHistorial.builder()
                .id(1L)
                .latitud(-33.45)
                .longitud(-70.66)
                .velocidadKmh(25.0)
                .precisionM(5.0)
                .build();
        when(deliveryService.obtenerRutaDelivery(1L)).thenReturn(List.of(punto));

        mockMvc.perform(get("/api/v1/delivery/1/ruta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitud").value(-33.45))
                .andExpect(jsonPath("$[0].velocidadKmh").value(25.0));

        verify(deliveryService).obtenerRutaDelivery(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/repartidor/{repId}/historial
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerHistorialDeRepartidor() throws Exception {
        when(deliveryService.historialRepartidor(3L)).thenReturn(List.of(deliveryEjemplo()));

        mockMvc.perform(get("/api/v1/delivery/repartidor/3/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedidoId").value(42))
                .andExpect(jsonPath("$[0]._links.self.href").exists());

        verify(deliveryService).historialRepartidor(3L);
    }
}