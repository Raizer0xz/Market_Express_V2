package com.example.MS_carrito.repository;

import com.example.MS_carrito.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    // Carrito activo de un usuario
    Optional<Carrito> findByUsuarioIdAndEstado(Long usuarioId, String estado);

    // Todos los carritos de un usuario
    List<Carrito> findByUsuarioId(Long usuarioId);
}