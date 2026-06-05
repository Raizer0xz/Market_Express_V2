package com.example.MS_usuarios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario extends RepresentationModel<Usuario> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "Entre 2 y 100 caracteres")
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email invalido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(min = 9, max = 15, message = "Telefono entre 9 y 15 caracteres")
    @Column(length = 15)
    private String telefono;

    @NotBlank(message = "El rol es obligatorio")
    @Column(nullable = false)
    private String rol;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) // false = no se puede modificar después de crearse
    private LocalDateTime createdAt;
}