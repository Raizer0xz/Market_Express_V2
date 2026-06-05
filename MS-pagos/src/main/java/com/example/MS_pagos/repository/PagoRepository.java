package com.example.MS_pagos.repository;

import com.example.MS_pagos.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByTransaccionId(String transaccionId);
    List<Pago> findByPedidoId(Long pedidoId);
}