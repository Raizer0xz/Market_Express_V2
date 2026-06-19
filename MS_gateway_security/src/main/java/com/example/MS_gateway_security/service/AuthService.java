package com.example.MS_gateway_security.service;

import com.example.MS_gateway_security.dto.*;
import com.example.MS_gateway_security.model.Credencial;
import com.example.MS_gateway_security.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // POST /auth/registrar
    public AuthResponse registrar(RegisterRequest request) {
        if (credencialRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Ya existe una cuenta con ese email");
        if (credencialRepository.existsByUsuarioId(request.getUsuarioId()))
            throw new RuntimeException("Ese usuario ya tiene credenciales");

        Credencial credencial = Credencial.builder()
                .usuarioId(request.getUsuarioId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .activo(true)
                .build();

        credencialRepository.save(credencial);

        String token = jwtService.generarToken(credencial.getEmail(), credencial.getUsuarioId(), credencial.getRol());
        return AuthResponse.builder().token(token).email(credencial.getEmail())
                .rol(credencial.getRol()).usuarioId(credencial.getUsuarioId())
                .mensaje("Usuario registrado exitosamente").build();
    }

    // POST /auth/login
    public AuthResponse login(LoginRequest request) {
        Credencial credencial = credencialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email o contrasena incorrectos"));

        if (!credencial.getActivo())
            throw new RuntimeException("Cuenta desactivada");

        // BCrypt compara texto plano vs hash almacenado
        if (!passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash()))
            throw new RuntimeException("Email o contrasena incorrectos");

        String token = jwtService.generarToken(credencial.getEmail(), credencial.getUsuarioId(), credencial.getRol());
        return AuthResponse.builder().token(token).email(credencial.getEmail())
                .rol(credencial.getRol()).usuarioId(credencial.getUsuarioId())
                .mensaje("Login exitoso").build();
    }

    // POST /auth/validar  (lo llaman los demas microservicios)
    public ValidarTokenResponse validarToken(String token) {
        try {
            if (!jwtService.esTokenValido(token))
                return ValidarTokenResponse.builder().valido(false).mensaje("Token expirado o invalido").build();

            return ValidarTokenResponse.builder()
                    .valido(true)
                    .usuarioId(jwtService.extraerUsuarioId(token))
                    .email(jwtService.extraerEmail(token))
                    .rol(jwtService.extraerRol(token))
                    .mensaje("Token valido").build();
        } catch (Exception e) {
            return ValidarTokenResponse.builder().valido(false).mensaje("Token invalido: " + e.getMessage()).build();
        }
    }
}
