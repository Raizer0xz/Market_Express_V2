package com.example.MS_sucursales.service;

import com.example.MS_sucursales.model.Sucursal;
import com.example.MS_sucursales.repository.SucursalRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SucursalService {
    @Autowired
    private SucursalRepository repository;

    // Listar todas las sucursales (para el administrador)
    @Transactional(readOnly = true)
    public List<Sucursal> obtenerTodas() {
        return repository.findAll();
    }

    // Listar solo las sucursales abiertas (para el cliente/delivery)
    @Transactional(readOnly = true)
    public List<Sucursal> obtenerAbiertas() {
        return repository.findByAbiertaTrue();
    }

    // Obtener una sucursal por ID con manejo de error si no existe
    @Transactional(readOnly = true)
    public Sucursal obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con el ID: " + id));
    }

    // Guardar o actualizar sucursal
    @Transactional
    public Sucursal guardar(Sucursal sucursal) {
        // Aquí puedes forzar lógica: ej. toda sucursal nueva empieza como 'abierta'
        return repository.save(sucursal);
    }

    // Borrar sucursal
    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Sucursal no existe");
        }
        repository.deleteById(id);
    }
}
