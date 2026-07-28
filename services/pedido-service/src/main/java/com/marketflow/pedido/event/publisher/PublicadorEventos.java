package com.marketflow.pedido.event.publisher;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.pedido.domain.OutboxEvent;
import com.marketflow.pedido.event.EnvelopeEvento;
import com.marketflow.pedido.repository.OutboxEventRepository;

@Component
public class PublicadorEventos {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PublicadorEventos(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(EnvelopeEvento<?> evento) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    evento.eventId(),
                    evento.eventType(),
                    evento.sagaId(),
                    objectMapper.writeValueAsString(evento)
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar evento " + evento.eventType(), exception);
        }
    }
}
