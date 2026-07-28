CREATE TABLE notificacoes (
    id UUID PRIMARY KEY,
    pedido_id VARCHAR(120) NOT NULL,
    tipo_evento VARCHAR(120) NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    canal VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    enviada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_notificacoes_canal CHECK (canal IN ('SMS')),
    CONSTRAINT chk_notificacoes_status CHECK (status IN ('ENVIADA', 'FALHOU'))
);

CREATE INDEX idx_notificacoes_pedido_enviada_em ON notificacoes(pedido_id, enviada_em);
CREATE INDEX idx_notificacoes_tipo_evento ON notificacoes(tipo_evento);

CREATE TABLE eventos_processados (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
