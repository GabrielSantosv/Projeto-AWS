package com.marketflow.fiscal.event.publisher;

import org.springframework.stereotype.Component;

import com.marketflow.fiscal.event.EnvelopeEvento;

@Component
public class PublicadorEventos {

    public void publicar(EnvelopeEvento<?> evento) {
        // Implementação de envio para o broker, mantida como stub para os testes.
    }
}
