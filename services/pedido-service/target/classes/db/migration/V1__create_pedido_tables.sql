CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    operador_id VARCHAR(120) NOT NULL,
    cliente_id VARCHAR(120),
    telefone_cliente VARCHAR(40),
    valor_total NUMERIC(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    motivo_cancelamento VARCHAR(120),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_pedidos_valor_total_non_negative CHECK (valor_total >= 0),
    CONSTRAINT chk_pedidos_status CHECK (
        status IN ('RASCUNHO', 'AGUARDANDO_PAGAMENTO', 'PAGO', 'PROCESSANDO', 'CANCELADO', 'COMPLETO')
    )
);

CREATE TABLE itens_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produto_id VARCHAR(120) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    CONSTRAINT chk_itens_pedido_quantidade_positive CHECK (quantidade > 0),
    CONSTRAINT chk_itens_pedido_preco_non_negative CHECK (preco_unitario >= 0),
    CONSTRAINT chk_itens_pedido_subtotal_non_negative CHECK (subtotal >= 0)
);

CREATE INDEX idx_itens_pedido_pedido_id ON itens_pedido(pedido_id);
CREATE INDEX idx_pedidos_status_criado_em ON pedidos(status, criado_em);

CREATE TABLE eventos_processados (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    evento_id VARCHAR(80) NOT NULL UNIQUE,
    tipo_evento VARCHAR(120) NOT NULL,
    saga_id VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    publicado BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_outbox_events_publicado CHECK (publicado IN (TRUE, FALSE))
);

CREATE INDEX idx_outbox_events_publicado_criado_em ON outbox_events(publicado, criado_em);
