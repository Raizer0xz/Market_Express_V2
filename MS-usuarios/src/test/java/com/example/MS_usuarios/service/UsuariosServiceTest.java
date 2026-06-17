package com.example.MS_usuarios.service;

import com.example.MS_usuarios.model.Usuario;
import com.example.MS_usuarios.repository.RepositoryUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // Mockito puro, sin levantar Spring → test ultra rápido
class ServiceUsuarioTest {

    @Mock
    private RepositoryUsuario repository;

    @InjectMocks
    private ServiceUsuario serviceUsuario;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .email("juan@mail.com")
                .passwordHash("hashed123")
                .telefono("912345678")
                .rol("CLIENTE")
                .build();
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------
    @Test
    void findAll_deberiaRetornarListaDeUsuarios() {
        when(repository.findAll()).thenReturn(List.of(usuario));

        List<Usuario> resultado = serviceUsuario.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEmail()).isEqualTo("juan@mail.com");
        verify(repository).findAll();
    }

    @Test
    void findAll_deberiaRetornarListaVaciaCuandoNoHayUsuarios() {
        when(repository.findAll()).thenReturn(List.of());

        List<Usuario> resultado = serviceUsuario.findAll();

        assertThat(resultado).isEmpty();
        verify(repository).findAll();
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------
    @Test
    void findById_deberiaRetornarUsuarioCuandoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = serviceUsuario.findById(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(1L);
        assertThat(resultado.get().getNombre()).isEqualTo("Juan Pérez");
        verify(repository).findById(1L);
    }

    @Test
    void findById_deberiaRetornarVacioCuandoNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = serviceUsuario.findById(99L);

        assertThat(resultado).isEmpty();
        verify(repository).findById(99L);
    }

    // -------------------------------------------------------------------------
    // findByEmail()
    // -------------------------------------------------------------------------
    @Test
    void findByEmail_deberiaRetornarUsuarioCuandoExiste() {
        when(repository.findByEmail("juan@mail.com")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = serviceUsuario.findByEmail("juan@mail.com");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail()).isEqualTo("juan@mail.com");
        verify(repository).findByEmail("juan@mail.com");
    }

    @Test
    void findByEmail_deberiaRetornarVacioCuandoNoExiste() {
        when(repository.findByEmail("noexiste@mail.com")).thenReturn(Optional.empty());

        Optional<Usuario> resultado = serviceUsuario.findByEmail("noexiste@mail.com");

        assertThat(resultado).isEmpty();
        verify(repository).findByEmail("noexiste@mail.com");
    }

    // -------------------------------------------------------------------------
    // findByRol()
    // -------------------------------------------------------------------------
    @Test
    void findByRol_deberiaRetornarUsuariosConEseRol() {
        Usuario admin = Usuario.builder()
                .id(2L).nombre("Admin").email("admin@mail.com")
                .passwordHash("h").rol("ADMIN").build();

        when(repository.findByRol("ADMIN")).thenReturn(List.of(admin));

        List<Usuario> resultado = serviceUsuario.findByRol("ADMIN");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getRol()).isEqualTo("ADMIN");
        verify(repository).findByRol("ADMIN");
    }

    // -------------------------------------------------------------------------
    // existsByEmail()
    // -------------------------------------------------------------------------
    @Test
    void existsByEmail_deberiaRetornarTrueCuandoEmailYaExiste() {
        when(repository.existsByEmail("juan@mail.com")).thenReturn(true);

        boolean existe = serviceUsuario.existsByEmail("juan@mail.com");

        assertThat(existe).isTrue();
        verify(repository).existsByEmail("juan@mail.com");
    }

    @Test
    void existsByEmail_deberiaRetornarFalseCuandoEmailNoExiste() {
        when(repository.existsByEmail("nuevo@mail.com")).thenReturn(false);

        boolean existe = serviceUsuario.existsByEmail("nuevo@mail.com");

        assertThat(existe).isFalse();
        verify(repository).existsByEmail("nuevo@mail.com");
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------
    @Test
    void save_deberiaGuardarYRetornarUsuario() {
        when(repository.save(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = serviceUsuario.save(usuario);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez");
        verify(repository).save(usuario);
    }

    // -------------------------------------------------------------------------
    // deleteById()
    // -------------------------------------------------------------------------
    @Test
    void deleteById_deberiaRetornarTrueCuandoUsuarioExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        doNothing().when(repository).delete(usuario);

        boolean resultado = serviceUsuario.deleteById(1L);

        assertThat(resultado).isTrue();
        verify(repository).findById(1L);
        verify(repository).delete(usuario);
    }

    @Test
    void deleteById_deberiaRetornarFalseCuandoUsuarioNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        boolean resultado = serviceUsuario.deleteById(99L);

        assertThat(resultado).isFalse();
        verify(repository).findById(99L);
        verify(repository, never()).delete(any());
    }
}