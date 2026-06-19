package com.example.MS_gateway_security.repository;

import com.example.MS_gateway_security.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredencialRepository extends JpaRepository<Credencial, Long> {
    Optional<Credencial> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsuarioId(Long usuarioId);
}
