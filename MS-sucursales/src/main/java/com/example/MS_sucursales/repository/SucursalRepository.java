package com.example.MS_sucursales.repository;

import com.example.MS_sucursales.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal,Long> {
    List<Sucursal> findByAbiertaTrue();
}
