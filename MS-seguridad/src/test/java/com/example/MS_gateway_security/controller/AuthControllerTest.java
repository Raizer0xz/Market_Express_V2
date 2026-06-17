package com.example.MS_gateway_security.controller;

import com.example.MS_gateway_security.dto.AuthResponse;
import com.example.MS_gateway_security.dto.ValidarTokenResponse;
import com.example.MS_gateway_security.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)   // desactiva Spring Security para los tests
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // -------------------------------------------------------------------------
    // Helper: AuthResponse de ejemplo
    // -------------------------------------------------------------------------
    private AuthResponse authResponseEjemplo() {
        return AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.token")
                .email("juan@mail.com")
                .rol("CLIENTE")
                .usuarioId(1L)
                .mensaje("Usuario registrado exitosamente")
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /auth/registrar → 201
    // -------------------------------------------------------------------------
    @Test
    void deberiaRegistrarCredencialesYRetornar201() throws Exception {
        when(authService.registrar(any())).thenReturn(authResponseEjemplo());

        String json = """
                {
                    "usuarioId": 1,
                    "email": "juan@mail.com",
                    "password": "segura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/auth/registrar")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente"));

        verify(authService).registrar(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/registrar → 409 email duplicado
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar409CuandoEmailYaExiste() throws Exception {
        when(authService.registrar(any()))
                .thenThrow(new RuntimeException("Ya existe una cuenta con ese email"));

        String json = """
                {
                    "usuarioId": 1,
                    "email": "juan@mail.com",
                    "password": "segura123",
                    "rol": "CLIENTE"
                }
                """;

        mockMvc.perform(post("/auth/registrar")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una cuenta con ese email"));

        verify(authService).registrar(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/registrar → 400 datos inválidos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoRegistroTieneDatosInvalidos() throws Exception {
        String json = """
                {
                    "email": "no-es-un-email",
                    "password": "corta"
                }
                """;

        mockMvc.perform(post("/auth/registrar")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registrar(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login → 200 con JWT
    // -------------------------------------------------------------------------
    @Test
    void deberiaLoginYRetornarJwt() throws Exception {
        AuthResponse loginResponse = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.token")
                .email("juan@mail.com")
                .rol("CLIENTE")
                .usuarioId(1L)
                .mensaje("Login exitoso")
                .build();

        when(authService.login(any())).thenReturn(loginResponse);

        String json = """
                {
                    "email": "juan@mail.com",
                    "password": "segura123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Login exitoso"));

        verify(authService).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login → 401 credenciales incorrectas
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar401CuandoCredencialesIncorrectas() throws Exception {
        when(authService.login(any()))
                .thenThrow(new RuntimeException("Email o contrasena incorrectos"));

        String json = """
                {
                    "email": "juan@mail.com",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contrasena incorrectos"));

        verify(authService).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login → 400 datos inválidos
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoLoginTieneDatosInvalidos() throws Exception {
        String json = """
                {
                    "email": "no-es-email"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /auth/validar → 200 token válido
    // -------------------------------------------------------------------------
    @Test
    void deberiaValidarTokenCorrectamente() throws Exception {
        ValidarTokenResponse validResponse = ValidarTokenResponse.builder()
                .valido(true)
                .usuarioId(1L)
                .email("juan@mail.com")
                .rol("CLIENTE")
                .mensaje("Token valido")
                .build();

        when(authService.validarToken(any())).thenReturn(validResponse);

        mockMvc.perform(post("/auth/validar")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.test.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Token valido"));

        verify(authService).validarToken("eyJhbGciOiJIUzI1NiJ9.test.token");
    }

    // -------------------------------------------------------------------------
    // POST /auth/validar → 401 token expirado/inválido
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar401CuandoTokenEsInvalido() throws Exception {
        ValidarTokenResponse invalidResponse = ValidarTokenResponse.builder()
                .valido(false)
                .mensaje("Token expirado o invalido")
                .build();

        when(authService.validarToken(any())).thenReturn(invalidResponse);

        mockMvc.perform(post("/auth/validar")
                        .header("Authorization", "Bearer token.expirado.invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.mensaje").value("Token expirado o invalido"));

        verify(authService).validarToken("token.expirado.invalido");
    }

    // -------------------------------------------------------------------------
    // POST /auth/validar → 400 header mal formado
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornar400CuandoHeaderAuthorizationEsMalFormado() throws Exception {
        mockMvc.perform(post("/auth/validar")
                        .header("Authorization", "sinBearer token123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.mensaje").value("Header invalido. Formato esperado: Bearer <token>"));

        verify(authService, never()).validarToken(any());
    }

    // -------------------------------------------------------------------------
    // GET /auth/health → 200
    // -------------------------------------------------------------------------
    @Test
    void deberiaRetornarHealthOk() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-seguridad"))
                .andExpect(jsonPath("$.estado").value("activo"))
                .andExpect(jsonPath("$.puerto").value("8082"));
    }
}