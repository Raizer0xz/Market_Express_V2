package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.MetricasRepartidorResponse;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.service.RepartidorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepartidorController.class)
@AutoConfigureMockMvc(addFilters = false)
class RepartidorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepartidorService repartidorService;

    private Repartidor repartidorEjemplo() {
        return Repartidor.builder()
                .id(1L)
                .nombre("Carlos Pérez")
                .telefono("987654321")
                .email("carlos@mail.com")
                .vehiculo(Repartidor.Vehiculo.MOTO)
                .estado(EstadoRepartidor.LIBRE)
                .activo(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores → 200 lista + HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarRepartidoresActivos() throws Exception {
        when(repartidorService.listarActivos()).thenReturn(List.of(repartidorEjemplo()));

        mockMvc.perform(get("/api/v1/repartidores").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.repartidorList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.repartidorList[0].nombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$._embedded.repartidorList[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.desactivar.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.metricas.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).listarActivos();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores → 204 sin repartidores
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoNoHayRepartidores() throws Exception {
        when(repartidorService.listarActivos()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repartidores"))
                .andExpect(status().isNoContent());

        verify(repartidorService).listarActivos();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerRepartidorPorId() throws Exception {
        when(repartidorService.obtenerPorId(1L)).thenReturn(repartidorEjemplo());

        mockMvc.perform(get("/api/v1/repartidores/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$.email").value("carlos@mail.com"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.metricas.href").exists())
                .andExpect(jsonPath("$._links.desactivar.href").exists());

        verify(repartidorService).obtenerPorId(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoRepartidorNoExiste() throws Exception {
        when(repartidorService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Repartidor no encontrado: 99"));

        mockMvc.perform(get("/api/v1/repartidores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));

        verify(repartidorService).obtenerPorId(99L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores → 201
    // -------------------------------------------------------------------------
    @Test
    void deberiaRegistrarRepartidor() throws Exception {
        when(repartidorService.registrar(any())).thenReturn(repartidorEjemplo());

        String json = """
                {
                    "nombre": "Carlos Pérez",
                    "telefono": "987654321",
                    "email": "carlos@mail.com",
                    "vehiculo": "MOTO"
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.metricas.href").exists());

        verify(repartidorService).registrar(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores → 409 email duplicado
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar409CuandoEmailYaExiste() throws Exception {
        when(repartidorService.registrar(any()))
                .thenThrow(new RuntimeException("Ya existe un repartidor con ese email"));

        String json = """
                {
                    "nombre": "Carlos",
                    "telefono": "987654321",
                    "email": "carlos@mail.com"
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un repartidor con ese email"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/repartidores/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarRepartidor() throws Exception {
        Repartidor actualizado = Repartidor.builder()
                .id(1L).nombre("Carlos Actualizado").telefono("999999999")
                .email("carlos@mail.com").vehiculo(Repartidor.Vehiculo.AUTO)
                .estado(EstadoRepartidor.LIBRE).activo(true).build();

        when(repartidorService.actualizar(eq(1L), any())).thenReturn(actualizado);

        String json = """
                {
                    "nombre": "Carlos Actualizado",
                    "telefono": "999999999",
                    "email": "carlos@mail.com",
                    "vehiculo": "AUTO"
                }
                """;

        mockMvc.perform(put("/api/v1/repartidores/1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Actualizado"))
                .andExpect(jsonPath("$.vehiculo").value("AUTO"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).actualizar(eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/repartidores/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaDesactivarRepartidor() throws Exception {
        doNothing().when(repartidorService).desactivar(1L);

        mockMvc.perform(delete("/api/v1/repartidores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Repartidor desactivado correctamente"));

        verify(repartidorService).desactivar(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/repartidores/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlDesactivarRepartidorInexistente() throws Exception {
        doThrow(new RuntimeException("Repartidor no encontrado: 99"))
                .when(repartidorService).desactivar(99L);

        mockMvc.perform(delete("/api/v1/repartidores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/repartidores/{id}/estado → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaCambiarEstadoDeRepartidor() throws Exception {
        doNothing().when(repartidorService).cambiarEstado(1L, EstadoRepartidor.OCUPADO);

        mockMvc.perform(patch("/api/v1/repartidores/1/estado")
                        .param("estado", "OCUPADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Estado actualizado a OCUPADO"));

        verify(repartidorService).cambiarEstado(1L, EstadoRepartidor.OCUPADO);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/repartidores/{id}/estado → 400 estado inválido
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoEstadoEsInvalido() throws Exception {
        mockMvc.perform(patch("/api/v1/repartidores/1/estado")
                        .param("estado", "VOLANDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Estado inválido: VOLANDO"));

        verify(repartidorService, never()).cambiarEstado(any(), any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores/{id}/ubicacion → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarUbicacionGPS() throws Exception {
        doNothing().when(repartidorService).actualizarUbicacion(eq(1L), any());

        String json = """
                {
                    "latitud": -33.4489,
                    "longitud": -70.6693,
                    "velocidadKmh": 30.0,
                    "precisionM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores/1/ubicacion")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Ubicación actualizada"));

        verify(repartidorService).actualizarUbicacion(eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id}/metricas → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerMetricasDeRepartidor() throws Exception {
        MetricasRepartidorResponse metricas = MetricasRepartidorResponse.builder()
                .repartidorId(1L)
                .nombre("Carlos Pérez")
                .totalEntregas(20)
                .entregasExitosas(18)
                .entregasFallidas(2)
                .tasaExito(90.0)
                .build();

        when(repartidorService.obtenerMetricas(1L)).thenReturn(metricas);

        mockMvc.perform(get("/api/v1/repartidores/1/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidorId").value(1))
                .andExpect(jsonPath("$.totalEntregas").value(20))
                .andExpect(jsonPath("$.entregasExitosas").value(18))
                .andExpect(jsonPath("$.tasaExito").value(90.0));

        verify(repartidorService).obtenerMetricas(1L);
    }
}