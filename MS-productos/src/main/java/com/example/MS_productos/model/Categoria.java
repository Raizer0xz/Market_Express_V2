package com.example.MS_productos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

@Data
@Entity
@Builder
@Table(name = "categoria")
@NoArgsConstructor
@AllArgsConstructor
public class Categoria extends RepresentationModel<Categoria> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @Column(name = "imagen_url")
    private String imagenUrl;
}