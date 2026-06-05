package com.example.MS_pedidos.repository;

import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAll();

    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    List<Pedido> findBySucursalIdAndEstado(Long sucursalId, EstadoPedido estado);
}
