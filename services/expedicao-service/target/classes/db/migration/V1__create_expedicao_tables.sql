CREATE TABLE separacoes (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    iniciada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    concluida_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_separacoes_status CHECK (status IN ('INICIADA', 'CONCLUIDA'))
);

CREATE INDEX idx_separacoes_status_iniciada_em ON separacoes(status, iniciada_em);

CREATE TABLE eventos_processados (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(80) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    saga_id VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    publicado BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_events_publicado_criado_em ON outbox_events(publicado, criado_em);
