package com.marketflow.pedido.event.publisher;

import org.springframework.stereotype.Component;

import com.marketflow.pedido.event.EnvelopeEvento;

@Component
public class PublicadorEventos {

    public void publicar(EnvelopeEvento<?> evento) {
        // Implementacao provisoria para testes e ambiente local.
    }
}
