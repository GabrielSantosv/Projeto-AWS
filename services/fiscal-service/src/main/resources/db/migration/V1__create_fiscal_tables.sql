CREATE TABLE notas_fiscais (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(120) NOT NULL UNIQUE,
    numero_nota VARCHAR(120) NOT NULL UNIQUE,
    valor_total NUMERIC(12,2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    emitida_em TIMESTAMP NOT NULL
);

CREATE TABLE pedidos_cancelados (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(120) NOT NULL UNIQUE,
    motivo VARCHAR(120) NOT NULL,
    cancelado_em TIMESTAMP NOT NULL
);

CREATE TABLE eventos_processados (
    event_id VARCHAR(120) PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL
);
