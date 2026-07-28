CREATE TABLE notas_fiscais (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(255) NOT NULL UNIQUE,
    numero_nota VARCHAR(255) NOT NULL UNIQUE,
    valor_total NUMERIC(12,2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    emitida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_notas_fiscais_valor_non_negative CHECK (valor_total >= 0),
    CONSTRAINT chk_notas_fiscais_status CHECK (status IN ('EMITIDA', 'CANCELADA'))
);

CREATE TABLE pedidos_cancelados (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(255) NOT NULL UNIQUE,
    motivo VARCHAR(120) NOT NULL,
    cancelado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE eventos_processados (
    event_id VARCHAR(120) PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    publicado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_fiscal_outbox_publicado CHECK (publicado IN (TRUE, FALSE))
);

CREATE INDEX idx_notas_fiscais_status ON notas_fiscais(status);
CREATE INDEX idx_pedidos_cancelados_motivo ON pedidos_cancelados(motivo);
CREATE INDEX idx_fiscal_outbox_publicado_criado_em ON outbox_events(publicado, criado_em);
