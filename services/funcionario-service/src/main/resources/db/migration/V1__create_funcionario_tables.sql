CREATE TABLE funcionarios (
    id UUID PRIMARY KEY,
    matricula VARCHAR(80) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    cargo VARCHAR(80) NOT NULL,
    senha_hash VARCHAR(128) NOT NULL,
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_funcionarios_nome_non_empty CHECK (char_length(trim(nome)) > 0),
    CONSTRAINT chk_funcionarios_cargo_non_empty CHECK (char_length(trim(cargo)) > 0)
);

CREATE INDEX idx_funcionarios_ativo_cargo ON funcionarios(ativo, cargo);

CREATE TABLE sessoes_caixa (
    id UUID PRIMARY KEY,
    funcionario_id UUID NOT NULL REFERENCES funcionarios(id),
    token_id VARCHAR(80) NOT NULL UNIQUE,
    iniciada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    ativa BOOLEAN NOT NULL,
    CONSTRAINT chk_sessoes_caixa_expiracao CHECK (expira_em > iniciada_em)
);

CREATE INDEX idx_sessoes_caixa_funcionario_ativa ON sessoes_caixa(funcionario_id, ativa);
CREATE INDEX idx_sessoes_caixa_token_ativa ON sessoes_caixa(token_id, ativa);
