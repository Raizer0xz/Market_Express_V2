package com.example.MS_usuarios.service;

import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuariosServiceTest {

    @Mock
    private PedidoRepository repository;

    @InjectMocks
    private PedidoService service;

    // FIX: El modelo Pedido usa setId() no setIdPedido(), y no tiene campo "descripcion".
    //      PedidoService no tiene findById(), pero sí findByUsuario() y findAll().
    //      Se reemplaza por un test que prueba findAll() y save(), que sí existen.

    @Test
    void deberiaRetornarTodosLosPedidos() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(5L);
        pedido.setSucursalId(2L);
        pedido.setDireccionEntrega("Av. Siempreviva 742");
        pedido.setTotal(new BigDecimal("1550.50"));
        pedido.setEstado(EstadoPedido.PENDIENTE);

        Mockito.when(repository.findAll())
                .thenReturn(List.of(pedido));

        List<Pedido> resultado = service.findAll();

        assertFalse(resultado.isEmpty());
        assertEquals(1L, resultado.get(0).getId());
        assertEquals(EstadoPedido.PENDIENTE, resultado.get(0).getEstado());

        verify(repository).findAll();
    }

    @Test
    void deberiaGuardarPedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(5L);
        pedido.setSucursalId(2L);
        pedido.setDireccionEntrega("Av. Siempreviva 742");
        pedido.setTotal(new BigDecimal("1550.50"));
        pedido.setEstado(EstadoPedido.PENDIENTE);

        Mockito.when(repository.save(pedido))
                .thenReturn(pedido);

        Pedido resultado = service.save(pedido);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());

        verify(repository).save(pedido);
    }

    @Test
    void deberiaActualizarEstadoDePedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setUsuarioId(5L);
        pedido.setSucursalId(2L);
        pedido.setDireccionEntrega("Av. Siempreviva 742");
        pedido.setTotal(new BigDecimal("1550.50"));

        Mockito.when(repository.findById(1L))
                .thenReturn(Optional.of(pedido));

        Pedido pedidoActualizado = new Pedido();
        pedidoActualizado.setId(1L);
        pedidoActualizado.setEstado(EstadoPedido.CONFIRMADO);
        pedidoActualizado.setUsuarioId(5L);
        pedidoActualizado.setSucursalId(2L);
        pedidoActualizado.setDireccionEntrega("Av. Siempreviva 742");
        pedidoActualizado.setTotal(new BigDecimal("1550.50"));

        Mockito.when(repository.save(pedido))
                .thenReturn(pedidoActualizado);

        Optional<Pedido> resultado = service.update(1L, EstadoPedido.CONFIRMADO);

        assertTrue(resultado.isPresent());
        assertEquals(EstadoPedido.CONFIRMADO, resultado.get().getEstado());

        verify(repository).findById(1L);
    }
}