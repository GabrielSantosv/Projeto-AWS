create table stock_movements (
    id uuid primary key,
    product_id uuid not null references products(id),
    type varchar(30) not null,
    quantity integer not null,
    previous_quantity_on_hand integer not null,
    new_quantity_on_hand integer not null,
    reason varchar(255) not null,
    created_at timestamp with time zone not null,
    constraint chk_stock_movements_type check (type in ('STOCK_IN', 'STOCK_OUT')),
    constraint chk_stock_movements_quantity_positive check (quantity > 0),
    constraint chk_stock_movements_quantities_non_negative check (
        previous_quantity_on_hand >= 0
        and new_quantity_on_hand >= 0
    )
);

create index idx_stock_movements_product_created_at on stock_movements(product_id, created_at desc);
