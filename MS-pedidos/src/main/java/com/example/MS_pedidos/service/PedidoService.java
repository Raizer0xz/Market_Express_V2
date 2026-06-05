package com.example.MS_pedidos.service;
import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class PedidoService {
    @Autowired
    private PedidoRepository repository;

    public List<Pedido> findAll() {
        return repository.findAll();
    }

    public List<Pedido> findByUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);
    }

    public List<Pedido> findBySucursalIdAndEstado(Long sucursalId, EstadoPedido estado) {
        return repository.findBySucursalIdAndEstado(sucursalId, estado);
    }

    public Pedido save(Pedido pedido) {
        return repository.save(pedido);
    }

    public Optional<Pedido> update(Long id, EstadoPedido nuevoEstado) {
        return repository.findById(id).map(p -> {
            p.setEstado(nuevoEstado);
            return repository.save(p);
        });
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

}
