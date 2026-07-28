package com.marketflow.expedicao.event.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.expedicao.domain.OutboxEvent;
import com.marketflow.expedicao.event.EnvelopeEvento;
import com.marketflow.expedicao.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
