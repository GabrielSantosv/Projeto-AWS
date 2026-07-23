package com.marketflow.pedido.event.publisher;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.pedido.domain.OutboxEvent;
import com.marketflow.pedido.domain.Pedido;
import com.marketflow.pedido.event.EnvelopeEvento;
import com.marketflow.pedido.repository.OutboxEventRepository;
import com.marketflow.pedido.repository.PedidoRepository;
import com.marketflow.pedido.service.PedidoService;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PedidoRepository pedidoRepository;
    private final PublicadorEventos publicadorEventos;
    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            PedidoRepository pedidoRepository,
            PublicadorEventos publicadorEventos,
            PedidoService pedidoService,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.pedidoRepository = pedidoRepository;
        this.publicadorEventos = publicadorEventos;
        this.pedidoService = pedidoService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms}")
    @Transactional
    public void publicarPendentes() {
        List<OutboxEvent> pendentes = outboxEventRepository.findPendentesForUpdate(Pageable.ofSize(10));
        for (OutboxEvent evento : pendentes) {
            EnvelopeEvento<?> envelope = lerEnvelope(evento);
            publicadorEventos.publicar(envelope);
            evento.marcarPublicado();
            outboxEventRepository.save(evento);
            Pedido pedido = pedidoRepository.findById(java.util.UUID.fromString(envelope.sagaId())).orElse(null);
            if (pedido != null) {
                pedidoService.marcarProcessandoAposPublicacaoSemTransacao(envelope.sagaId());
            }
        }
    }

    private EnvelopeEvento<?> lerEnvelope(OutboxEvent evento) {
        try {
            return objectMapper.readValue(evento.getPayloadJson(), new TypeReference<EnvelopeEvento<Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel ler envelope do outbox", ex);
        }
    }
}
