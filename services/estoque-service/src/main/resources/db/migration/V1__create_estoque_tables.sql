create table produtos (
    produto_id varchar(120) primary key,
    nome varchar(255) not null,
    preco_unitario numeric(12, 2) not null,
    quantidade_disponivel_total integer not null,
    quantidade_reservada integer not null,
    limite_estoque_baixo integer not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    constraint chk_produtos_quantidades_non_negative check (
        quantidade_disponivel_total >= 0
        and quantidade_reservada >= 0
        and limite_estoque_baixo >= 0
        and quantidade_reservada <= quantidade_disponivel_total
    )
);

create table reservas_estoque (
    id uuid primary key,
    pedido_id varchar(120) not null,
    produto_id varchar(120) not null references produtos(produto_id),
    quantidade integer not null,
    status varchar(30) not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    constraint uk_reservas_estoque_pedido_produto unique (pedido_id, produto_id),
    constraint chk_reservas_estoque_quantidade_positive check (quantidade > 0),
    constraint chk_reservas_estoque_status check (status in ('RESERVADA', 'CANCELADA'))
);

create index idx_reservas_estoque_pedido_id on reservas_estoque(pedido_id);
create index idx_reservas_estoque_pedido_status on reservas_estoque(pedido_id, status);

create table eventos_processados (
    event_id varchar(80) primary key,
    event_type varchar(120) not null,
    processado_em timestamp with time zone not null
);

create table outbox_events (
    id uuid primary key,
    event_id varchar(80) not null unique,
    event_type varchar(120) not null,
    saga_id varchar(120) not null,
    correlation_id varchar(120) not null,
    payload text not null,
    status varchar(30) not null,
    tentativas integer not null,
    ultimo_erro text,
    criado_em timestamp with time zone not null,
    publicado_em timestamp with time zone,
    constraint chk_outbox_events_status check (status in ('PENDENTE', 'PUBLICADO')),
    constraint chk_outbox_events_tentativas check (tentativas >= 0)
);

create index idx_outbox_events_status_criado_em on outbox_events(status, criado_em);
