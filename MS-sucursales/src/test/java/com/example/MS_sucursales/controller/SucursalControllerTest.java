package com.example.MS_sucursales.controller;

import com.example.MS_sucursales.model.Sucursal;
import com.example.MS_sucursales.service.SucursalService;
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

@WebMvcTest(SucursalController.class)
@AutoConfigureMockMvc(addFilters = false)
class SucursalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SucursalService sucursalService;

    private Sucursal sucursalEjemplo() {
        return Sucursal.builder()
                .id(1L)
                .nombre("Sucursal Centro")
                .direccion("Av. Libertador 123")
                .latitud(-33.4489)
                .longitud(-70.6693)
                .horarioApertura("08:00")
                .horarioCierre("22:00")
                .abierta(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales → 200 con lista + HATEOAS
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarTodasLasSucursales() throws Exception {
        when(sucursalService.obtenerTodas()).thenReturn(List.of(sucursalEjemplo()));

        mockMvc.perform(get("/api/v1/sucursales").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.sucursalList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.sucursalList[0].nombre").value("Sucursal Centro"))
                .andExpect(jsonPath("$._embedded.sucursalList[0].abierta").value(true))
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.todas.href").exists())
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.delete.href").exists())
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.cambiar-estado.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.abiertas.href").exists());

        verify(sucursalService).obtenerTodas();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales → 204 cuando lista vacía
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar204CuandoNoHaySucursales() throws Exception {
        when(sucursalService.obtenerTodas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sucursales"))
                .andExpect(status().isNoContent());

        verify(sucursalService).obtenerTodas();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/abiertas → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarSucursalesAbiertas() throws Exception {
        when(sucursalService.obtenerAbiertas()).thenReturn(List.of(sucursalEjemplo()));

        mockMvc.perform(get("/api/v1/sucursales/abiertas").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.sucursalList[0].abierta").value(true))
                .andExpect(jsonPath("$._embedded.sucursalList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todas.href").exists());

        verify(sucursalService).obtenerAbiertas();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/{id} → 200 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarSucursalPorId() throws Exception {
        when(sucursalService.obtenerPorId(1L)).thenReturn(sucursalEjemplo());

        mockMvc.perform(get("/api/v1/sucursales/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Sucursal Centro"))
                .andExpect(jsonPath("$.latitud").value(-33.4489))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todas.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.cambiar-estado.href").exists());

        verify(sucursalService).obtenerPorId(1L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404CuandoSucursalNoExiste() throws Exception {
        when(sucursalService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Sucursal no encontrada con el ID: 99"));

        mockMvc.perform(get("/api/v1/sucursales/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sucursal no encontrada con el ID: 99"));

        verify(sucursalService).obtenerPorId(99L);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sucursales → 201 con links
    // -------------------------------------------------------------------------
    @Test
    void deberiaCrearSucursal() throws Exception {
        when(sucursalService.guardar(any(Sucursal.class))).thenReturn(sucursalEjemplo());

        String json = """
                {
                    "nombre": "Sucursal Centro",
                    "direccion": "Av. Libertador 123",
                    "latitud": -33.4489,
                    "longitud": -70.6693,
                    "horarioApertura": "08:00",
                    "horarioCierre": "22:00"
                }
                """;

        mockMvc.perform(post("/api/v1/sucursales")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Sucursal Centro"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.todas.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(sucursalService).guardar(any(Sucursal.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sucursales → 400 si faltan campos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoFaltanCamposObligatorios() throws Exception {
        String json = """
                {
                    "nombre": "Sin datos"
                }
                """;

        mockMvc.perform(post("/api/v1/sucursales")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/sucursales/{id} → 200 actualizado + links
    // -------------------------------------------------------------------------
    @Test
    void deberiaActualizarSucursal() throws Exception {
        Sucursal actualizada = Sucursal.builder()
                .id(1L)
                .nombre("Sucursal Norte")
                .direccion("Av. Norte 456")
                .latitud(-33.40)
                .longitud(-70.60)
                .horarioApertura("09:00")
                .horarioCierre("21:00")
                .abierta(true)
                .build();

        when(sucursalService.obtenerPorId(1L)).thenReturn(sucursalEjemplo());
        when(sucursalService.guardar(any(Sucursal.class))).thenReturn(actualizada);

        String json = """
                {
                    "nombre": "Sucursal Norte",
                    "direccion": "Av. Norte 456",
                    "latitud": -33.40,
                    "longitud": -70.60,
                    "horarioApertura": "09:00",
                    "horarioCierre": "21:00"
                }
                """;

        mockMvc.perform(put("/api/v1/sucursales/1")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sucursal Norte"))
                .andExpect(jsonPath("$.direccion").value("Av. Norte 456"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());

        verify(sucursalService).obtenerPorId(1L);
        verify(sucursalService).guardar(any(Sucursal.class));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/sucursales/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlActualizarSucursalInexistente() throws Exception {
        when(sucursalService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Sucursal no encontrada con el ID: 99"));

        String json = """
                {
                    "nombre": "X",
                    "direccion": "X",
                    "latitud": -33.0,
                    "longitud": -70.0,
                    "horarioApertura": "08:00",
                    "horarioCierre": "22:00"
                }
                """;

        mockMvc.perform(put("/api/v1/sucursales/99")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sucursal no encontrada con el ID: 99"));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/sucursales/{id}/estado → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaCarmbiarEstadoDeSucursal() throws Exception {
        when(sucursalService.obtenerPorId(1L)).thenReturn(sucursalEjemplo());
        when(sucursalService.guardar(any(Sucursal.class))).thenReturn(sucursalEjemplo());

        mockMvc.perform(patch("/api/v1/sucursales/1/estado")
                        .param("abierta", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sucursal cerrada correctamente"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.abierta").value(false));

        verify(sucursalService).obtenerPorId(1L);
        verify(sucursalService).guardar(any(Sucursal.class));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/sucursales/{id} → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaEliminarSucursal() throws Exception {
        doNothing().when(sucursalService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/sucursales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sucursal eliminada correctamente"));

        verify(sucursalService).eliminar(1L);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/sucursales/{id} → 404
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar404AlEliminarSucursalInexistente() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar: Sucursal no existe"))
                .when(sucursalService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/sucursales/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se puede eliminar: Sucursal no existe"));

        verify(sucursalService).eliminar(99L);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/health → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHealthOk() throws Exception {
        mockMvc.perform(get("/api/v1/sucursales/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-sucursales"))
                .andExpect(jsonPath("$.estado").value("activo"));
    }
}