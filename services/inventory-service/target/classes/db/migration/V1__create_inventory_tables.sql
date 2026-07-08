create table products (
    id uuid primary key,
    sku varchar(120) not null unique,
    name varchar(255) not null,
    unit_price numeric(12, 2) not null,
    quantity_on_hand integer not null,
    quantity_reserved integer not null,
    low_stock_threshold integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint chk_products_quantities_non_negative check (
        quantity_on_hand >= 0
        and quantity_reserved >= 0
        and low_stock_threshold >= 0
        and quantity_on_hand >= quantity_reserved
    )
);

create table stock_reservations (
    id uuid primary key,
    order_id varchar(120) not null,
    product_id uuid not null references products(id),
    quantity integer not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_stock_reservations_order_product unique (order_id, product_id),
    constraint chk_stock_reservations_quantity_positive check (quantity > 0),
    constraint chk_stock_reservations_status check (status in ('RESERVED', 'CONFIRMED', 'RELEASED'))
);

create index idx_stock_reservations_order_id on stock_reservations(order_id);
create index idx_stock_reservations_order_status on stock_reservations(order_id, status);

create table processed_events (
    event_id varchar(80) primary key,
    event_type varchar(120) not null,
    processed_at timestamp with time zone not null
);
