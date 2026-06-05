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
public class ValidarTokenResponse {

    @NotNull(message = "El estado de validez es obligatorio")
    private boolean valido; // Al ser primitivo 'boolean', por defecto es false, pero @NotNull asegura consistencia si se usa Boolean

    @NotNull(message = "El ID de usuario no puede ser nulo")
    private Long usuarioId;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email es inválido")
    private String email;

    @NotBlank(message = "El rol no puede estar vacío")
    private String rol;

    private String mensaje; // Opcional (ej. "Token expirado", "Token válido")
}