package com.example.MS_sucursales.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sucursales")
public class Sucursal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "La dirección no puede estar vacía")
    private String direccion;

    @NotNull(message = "La latitud es requerida para geolocalización")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitud;

    @NotNull(message = "La longitud es requerida para geolocalización")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitud;

    @NotBlank(message = "El horario de apertura es obligatorio")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora debe ser HH:mm")
    @Column(name = "horario_apertura")
    private String horarioApertura;

    @NotBlank(message = "El horario de cierre es obligatorio")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora debe ser HH:mm")
    @Column(name = "horario_cierre")
    private String horarioCierre;

    private boolean abierta = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
