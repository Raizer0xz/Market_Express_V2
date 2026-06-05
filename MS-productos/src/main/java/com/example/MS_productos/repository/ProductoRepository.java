package com.example.MS_productos.repository;

import com.example.MS_productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaIdAndActivoTrue(Long categoriaId);

    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
}
