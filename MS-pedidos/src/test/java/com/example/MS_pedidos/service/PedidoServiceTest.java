package com.example.MS_pedidos.service;

import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @InjectMocks
    private PedidoService service;

    @Test
    void deberiaRetornarPedidoCuandoExiste() {

        Pedido pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setDescripcion("producto ");

        Mockito.when(repository.findById(1L))
                .thenReturn(Optional.of(pedido));

        Optional<Pedido> resultado = service.findById(1L);

        assertTrue(resultado.isPresent());
        assertEquals("producto", resultado.get().getDescripcion());

        verify(repository).findById(1L);
    }
}