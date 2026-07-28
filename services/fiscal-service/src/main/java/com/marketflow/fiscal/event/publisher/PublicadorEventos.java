package com.marketflow.fiscal.event.publisher;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.fiscal.domain.OutboxEvent;
import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.repository.OutboxEventRepository;

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
            outboxEventRepository.save(new OutboxEvent(evento, objectMapper.writeValueAsString(evento)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar evento " + evento.eventType(), exception);
        }
    }
}
