package com.marketflow.pedido.event.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.pedido.domain.OutboxEvent;
import com.marketflow.pedido.repository.OutboxEventRepository;
import com.marketflow.pedido.service.PedidoService;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PublicadorBroker publicadorBroker;
    private final PedidoService pedidoService;

    @Value("${outbox.batch-size:10}")
    private int batchSize;

    @Value("${outbox.polling-enabled:true}")
    private boolean pollingEnabled;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            PublicadorBroker publicadorBroker,
            PedidoService pedidoService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.publicadorBroker = publicadorBroker;
        this.pedidoService = pedidoService;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms}")
    @Transactional
    public void poll() {
        if (pollingEnabled) {
            publicarPendentes();
        }
    }

    @Transactional
    public void publicarPendentes() {
        List<OutboxEvent> pendentes = outboxEventRepository.findPendentesForUpdate(Pageable.ofSize(batchSize));
        pendentes.forEach(this::publicarNoBroker);
    }

    private void publicarNoBroker(OutboxEvent evento) {
        try {
            publicadorBroker.publicar(evento);
            pedidoService.marcarProcessandoAposPublicacaoSemTransacao(evento.getSagaId());
            evento.marcarPublicado();
            outboxEventRepository.save(evento);
        } catch (RuntimeException exception) {
            log.warn("Falha ao publicar evento {} no SNS; permanecera no outbox.", evento.getEventoId(), exception);
        }
    }
}
