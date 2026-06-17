package com.example.MS_gateway_security.service;

import com.example.MS_gateway_security.dto.AuthResponse;
import com.example.MS_gateway_security.dto.LoginRequest;
import com.example.MS_gateway_security.dto.RegisterRequest;
import com.example.MS_gateway_security.dto.ValidarTokenResponse;
import com.example.MS_gateway_security.model.Credencial;
import com.example.MS_gateway_security.repository.CredencialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredencialRepository credencialRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Credencial credencial;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        credencial = Credencial.builder()
                .id(1L)
                .usuarioId(10L)
                .email("juan@mail.com")
                .passwordHash("$2a$10$hasheado")
                .rol("CLIENTE")
                .activo(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsuarioId(10L);
        registerRequest.setEmail("juan@mail.com");
        registerRequest.setPassword("segura123");
        registerRequest.setRol("CLIENTE");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@mail.com");
        loginRequest.setPassword("segura123");
    }

    // -------------------------------------------------------------------------
    // registrar()
    // -------------------------------------------------------------------------
    @Test
    void registrar_deberiaCrearCredencialesYRetornarJwt() {
        when(credencialRepository.existsByEmail("juan@mail.com")).thenReturn(false);
        when(credencialRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(passwordEncoder.encode("segura123")).thenReturn("$2a$10$hasheado");
        when(credencialRepository.save(any(Credencial.class))).thenReturn(credencial);
        when(jwtService.generarToken("juan@mail.com", 10L, "CLIENTE"))
                .thenReturn("eyJ.test.token");

        AuthResponse response = authService.registrar(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("eyJ.test.token");
        assertThat(response.getEmail()).isEqualTo("juan@mail.com");
        assertThat(response.getRol()).isEqualTo("CLIENTE");
        assertThat(response.getUsuarioId()).isEqualTo(10L);
        assertThat(response.getMensaje()).isEqualTo("Usuario registrado exitosamente");

        verify(credencialRepository).existsByEmail("juan@mail.com");
        verify(credencialRepository).existsByUsuarioId(10L);
        verify(credencialRepository).save(any(Credencial.class));
        verify(jwtService).generarToken("juan@mail.com", 10L, "CLIENTE");
    }

    @Test
    void registrar_deberiaLanzarExcepcionCuandoEmailYaExiste() {
        when(credencialRepository.existsByEmail("juan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ya existe una cuenta con ese email");

        verify(credencialRepository).existsByEmail("juan@mail.com");
        verify(credencialRepository, never()).save(any());
    }

    @Test
    void registrar_deberiaLanzarExcepcionCuandoUsuarioYaTieneCredenciales() {
        when(credencialRepository.existsByEmail("juan@mail.com")).thenReturn(false);
        when(credencialRepository.existsByUsuarioId(10L)).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ese usuario ya tiene credenciales");

        verify(credencialRepository).existsByUsuarioId(10L);
        verify(credencialRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------
    @Test
    void login_deberiaAutenticarYRetornarJwt() {
        when(credencialRepository.findByEmail("juan@mail.com")).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("segura123", "$2a$10$hasheado")).thenReturn(true);
        when(jwtService.generarToken("juan@mail.com", 10L, "CLIENTE"))
                .thenReturn("eyJ.test.token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("eyJ.test.token");
        assertThat(response.getEmail()).isEqualTo("juan@mail.com");
        assertThat(response.getMensaje()).isEqualTo("Login exitoso");

        verify(credencialRepository).findByEmail("juan@mail.com");
        verify(passwordEncoder).matches("segura123", "$2a$10$hasheado");
        verify(jwtService).generarToken("juan@mail.com", 10L, "CLIENTE");
    }

    @Test
    void login_deberiaLanzarExcepcionCuandoEmailNoExiste() {
        when(credencialRepository.findByEmail("noexiste@mail.com")).thenReturn(Optional.empty());

        loginRequest.setEmail("noexiste@mail.com");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email o contrasena incorrectos");

        verify(credencialRepository).findByEmail("noexiste@mail.com");
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_deberiaLanzarExcepcionCuandoPasswordEsIncorrecta() {
        when(credencialRepository.findByEmail("juan@mail.com")).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("wrongpass", "$2a$10$hasheado")).thenReturn(false);

        loginRequest.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email o contrasena incorrectos");

        verify(passwordEncoder).matches("wrongpass", "$2a$10$hasheado");
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_deberiaLanzarExcepcionCuandoCuentaEstaDesactivada() {
        credencial.setActivo(false);
        when(credencialRepository.findByEmail("juan@mail.com")).thenReturn(Optional.of(credencial));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cuenta desactivada");

        verify(credencialRepository).findByEmail("juan@mail.com");
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // validarToken()
    // -------------------------------------------------------------------------
    @Test
    void validarToken_deberiaRetornarValidoCuandoTokenEsValido() {
        when(jwtService.esTokenValido("token.valido")).thenReturn(true);
        when(jwtService.extraerUsuarioId("token.valido")).thenReturn(10L);
        when(jwtService.extraerEmail("token.valido")).thenReturn("juan@mail.com");
        when(jwtService.extraerRol("token.valido")).thenReturn("CLIENTE");

        ValidarTokenResponse response = authService.validarToken("token.valido");

        assertThat(response.isValido()).isTrue();
        assertThat(response.getUsuarioId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("juan@mail.com");
        assertThat(response.getRol()).isEqualTo("CLIENTE");
        assertThat(response.getMensaje()).isEqualTo("Token valido");

        verify(jwtService).esTokenValido("token.valido");
        verify(jwtService).extraerUsuarioId("token.valido");
        verify(jwtService).extraerEmail("token.valido");
        verify(jwtService).extraerRol("token.valido");
    }

    @Test
    void validarToken_deberiaRetornarInvalidoCuandoTokenEstaExpirado() {
        when(jwtService.esTokenValido("token.expirado")).thenReturn(false);

        ValidarTokenResponse response = authService.validarToken("token.expirado");

        assertThat(response.isValido()).isFalse();
        assertThat(response.getMensaje()).isEqualTo("Token expirado o invalido");

        verify(jwtService).esTokenValido("token.expirado");
        verify(jwtService, never()).extraerEmail(anyString());
    }

    @Test
    void validarToken_deberiaRetornarInvalidoCuandoJwtServiceLanzaExcepcion() {
        when(jwtService.esTokenValido("token.corrupto"))
                .thenThrow(new RuntimeException("firma invalida"));

        ValidarTokenResponse response = authService.validarToken("token.corrupto");

        assertThat(response.isValido()).isFalse();
        assertThat(response.getMensaje()).contains("Token invalido");
    }
}