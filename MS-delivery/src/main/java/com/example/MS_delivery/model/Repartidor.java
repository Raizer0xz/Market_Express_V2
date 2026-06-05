package com.example.MS_delivery.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repartidor")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Repartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 9, max = 15)
    @Column(nullable = false, length = 15)
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Vehiculo vehiculo = Vehiculo.MOTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoRepartidor estado = EstadoRepartidor.LIBRE;

    // Última posición conocida — se sobreescribe en cada ping GPS
    @Column
    private Double latitud;

    @Column
    private Double longitud;
    
    @Column(name = "ultima_ubicacion")
    private LocalDateTime ultimaUbicacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enums internos ───────────────────────────────────────────────────────

    public enum EstadoRepartidor { LIBRE, OCUPADO, INACTIVO }

    public enum Vehiculo { MOTO, BICICLETA, AUTO, A_PIE }
}
