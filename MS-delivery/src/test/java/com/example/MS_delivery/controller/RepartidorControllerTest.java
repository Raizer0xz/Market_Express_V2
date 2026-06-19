package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.MetricasRepartidorResponse;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.model.Repartidor.Vehiculo;
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
                .nombre("Carlos Perez")
                .telefono("987654321")
                .email("carlos@mail.com")
                .vehiculo(Vehiculo.MOTO)
                .estado(EstadoRepartidor.LIBRE)
                .activo(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarRepartidoresActivos() throws Exception {
        when(repartidorService.listarActivos()).thenReturn(List.of(repartidorEjemplo()));

        mockMvc.perform(get("/api/v1/repartidores").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.repartidorList[0].nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.todos.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.disponibles.href").exists())
                .andExpect(jsonPath("$._embedded.repartidorList[0]._links.metricas.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).listarActivos();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerRepartidorPorId() throws Exception {
        when(repartidorService.obtenerPorId(1L)).thenReturn(repartidorEjemplo());

        mockMvc.perform(get("/api/v1/repartidores/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$.email").value("carlos@mail.com"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todos.href").exists())
                .andExpect(jsonPath("$._links.disponibles.href").exists())
                .andExpect(jsonPath("$._links.metricas.href").exists());

        verify(repartidorService).obtenerPorId(1L);
    }

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
    // GET /api/v1/repartidores/disponibles
    // -------------------------------------------------------------------------
    @Test
    void deberiaListarRepartidoresDisponibles() throws Exception {
        Repartidor ocupado = repartidorEjemplo();
        ocupado.setId(2L);
        ocupado.setEstado(EstadoRepartidor.OCUPADO);

        when(repartidorService.listarActivos()).thenReturn(List.of(repartidorEjemplo(), ocupado));

        mockMvc.perform(get("/api/v1/repartidores/disponibles").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.repartidorList.length()").value(1))
                .andExpect(jsonPath("$._embedded.repartidorList[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).listarActivos();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id}/metricas
    // -------------------------------------------------------------------------
    @Test
    void deberiaObtenerMetricasDeRepartidor() throws Exception {
        MetricasRepartidorResponse metricas = MetricasRepartidorResponse.builder()
                .repartidorId(1L)
                .nombre("Carlos Perez")
                .totalEntregas(10)
                .entregasExitosas(8)
                .entregasFallidas(2)
                .tasaExito(80.0)
                .build();
        when(repartidorService.obtenerMetricas(1L)).thenReturn(metricas);

        mockMvc.perform(get("/api/v1/repartidores/1/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntregas").value(10))
                .andExpect(jsonPath("$.tasaExito").value(80.0));

        verify(repartidorService).obtenerMetricas(1L);
    }

    @Test
    void deberiaRetornar404AlObtenerMetricasDeRepartidorInexistente() throws Exception {
        when(repartidorService.obtenerMetricas(99L))
                .thenThrow(new RuntimeException("Repartidor no encontrado: 99"));

        mockMvc.perform(get("/api/v1/repartidores/99/metricas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores
    // -------------------------------------------------------------------------
    @Test
    void deberiaRegistrarRepartidor() throws Exception {
        when(repartidorService.registrar(any())).thenReturn(repartidorEjemplo());

        String json = """
                {
                    "nombre": "Carlos Perez",
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
                .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).registrar(any());
    }

    @Test
    void deberiaRetornar400AlRegistrarConEmailDuplicado() throws Exception {
        when(repartidorService.registrar(any()))
                .thenThrow(new RuntimeException("Ya existe un repartidor con ese email"));

        String json = """
                {
                    "nombre": "Carlos Perez",
                    "telefono": "987654321",
                    "email": "carlos@mail.com"
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe un repartidor con ese email"));
    }

    @Test
    void deberiaRetornar400AlRegistrarConDatosInvalidos() throws Exception {
        String json = """
                {
                    "telefono": "987654321"
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(repartidorService, never()).registrar(any());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarRepartidor() throws Exception {
        Repartidor actualizado = repartidorEjemplo();
        actualizado.setNombre("Carlos Perez Gomez");
        when(repartidorService.actualizar(eq(1L), any())).thenReturn(actualizado);

        String json = """
                {
                    "nombre": "Carlos Perez Gomez",
                    "telefono": "987654321",
                    "email": "carlos@mail.com",
                    "vehiculo": "AUTO"
                }
                """;

        mockMvc.perform(put("/api/v1/repartidores/1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Perez Gomez"))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(repartidorService).actualizar(eq(1L), any());
    }

    @Test
    void deberiaRetornar404AlActualizarRepartidorInexistente() throws Exception {
        when(repartidorService.actualizar(eq(99L), any()))
                .thenThrow(new RuntimeException("Repartidor no encontrado: 99"));

        String json = """
                {
                    "nombre": "X",
                    "telefono": "123456789",
                    "email": "x@mail.com"
                }
                """;

        mockMvc.perform(put("/api/v1/repartidores/99")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/repartidores/{id}/estado
    // -------------------------------------------------------------------------
    @Test
    void deberiaCambiarEstadoDeRepartidor() throws Exception {
        doNothing().when(repartidorService).cambiarEstado(1L, EstadoRepartidor.OCUPADO);

        String json = """
                { "estado": "OCUPADO" }
                """;

        mockMvc.perform(put("/api/v1/repartidores/1/estado")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Estado actualizado a OCUPADO"));

        verify(repartidorService).cambiarEstado(1L, EstadoRepartidor.OCUPADO);
    }

    @Test
    void deberiaRetornar400AlCambiarAEstadoInvalido() throws Exception {
        String json = """
                { "estado": "VOLANDO" }
                """;

        mockMvc.perform(put("/api/v1/repartidores/1/estado")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Estado inválido. Valores: LIBRE, OCUPADO, INACTIVO"));

        verify(repartidorService, never()).cambiarEstado(any(), any());
    }

    @Test
    void deberiaRetornar404AlCambiarEstadoDeRepartidorInexistente() throws Exception {
        doThrow(new RuntimeException("Repartidor no encontrado: 99"))
                .when(repartidorService).cambiarEstado(99L, EstadoRepartidor.LIBRE);

        String json = """
                { "estado": "LIBRE" }
                """;

        mockMvc.perform(put("/api/v1/repartidores/99/estado")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores/{id}/ubicacion
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarUbicacionDeRepartidor() throws Exception {
        doNothing().when(repartidorService).actualizarUbicacion(eq(1L), any());

        String json = """
                {
                    "latitud": -33.45,
                    "longitud": -70.66,
                    "velocidadKmh": 25.0,
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

    @Test
    void deberiaRetornar404AlActualizarUbicacionDeRepartidorInexistente() throws Exception {
        doThrow(new RuntimeException("Repartidor no encontrado: 99"))
                .when(repartidorService).actualizarUbicacion(eq(99L), any());

        String json = """
                {
                    "latitud": -33.45,
                    "longitud": -70.66
                }
                """;

        mockMvc.perform(post("/api/v1/repartidores/99/ubicacion")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));
    }

    @Test
    void deberiaRetornar400AlActualizarUbicacionConDatosInvalidos() throws Exception {
        String json = """
                { "velocidadKmh": 25.0 }
                """;

        mockMvc.perform(post("/api/v1/repartidores/1/ubicacion")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(repartidorService, never()).actualizarUbicacion(any(), any());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Test
    void deberiaDesactivarRepartidor() throws Exception {
        doNothing().when(repartidorService).desactivar(1L);

        mockMvc.perform(delete("/api/v1/repartidores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Repartidor desactivado"));

        verify(repartidorService).desactivar(1L);
    }

    @Test
    void deberiaRetornar404AlDesactivarRepartidorInexistente() throws Exception {
        doThrow(new RuntimeException("Repartidor no encontrado: 99"))
                .when(repartidorService).desactivar(99L);

        mockMvc.perform(delete("/api/v1/repartidores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Repartidor no encontrado: 99"));

        verify(repartidorService).desactivar(99L);
    }
}