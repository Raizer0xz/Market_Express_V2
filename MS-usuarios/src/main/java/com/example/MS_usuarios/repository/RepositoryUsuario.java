package com.example.MS_usuarios.repository;

import com.example.MS_usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RepositoryUsuario extends JpaRepository<Usuario,Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(String rol);

    boolean existsByEmail(String email);
}
