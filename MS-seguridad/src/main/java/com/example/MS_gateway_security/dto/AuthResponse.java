package com.example.MS_gateway_security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @NotBlank(message = "El token no puede estar vacío")
    private String token;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email en la respuesta es inválido")
    private String email;

    @NotBlank(message = "El rol no puede estar vacío")
    private String rol;

    @NotNull(message = "El ID de usuario no puede ser nulo")
    private Long usuarioId;

    private String mensaje; // Opcional, puede ser nulo en respuestas exitosas
}