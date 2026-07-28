CREATE TABLE fornecedores (
    id UUID PRIMARY KEY,
    produto_id VARCHAR(120) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    preco NUMERIC(12, 2) NOT NULL,
    prazo_dias INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_fornecedores_preco_non_negative CHECK (preco >= 0),
    CONSTRAINT chk_fornecedores_prazo_non_negative CHECK (prazo_dias >= 0)
);

CREATE INDEX idx_fornecedores_produto_ativo_preco ON fornecedores(produto_id, ativo, preco);

CREATE TABLE ordens_compra (
    id UUID PRIMARY KEY,
    produto_id VARCHAR(120) NOT NULL,
    fornecedor_id UUID NOT NULL REFERENCES fornecedores(id),
    quantidade INTEGER NOT NULL,
    preco NUMERIC(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmada_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_ordens_compra_quantidade_positive CHECK (quantidade > 0),
    CONSTRAINT chk_ordens_compra_preco_non_negative CHECK (preco >= 0),
    CONSTRAINT chk_ordens_compra_status CHECK (status IN ('GERADA', 'CONFIRMADA'))
);

CREATE INDEX idx_ordens_compra_produto_criada_em ON ordens_compra(produto_id, criada_em);

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
