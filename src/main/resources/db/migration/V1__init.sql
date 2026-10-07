-- Initial schema (docs/architecture.md section 6, docs/domain.md).
-- Money and quantities use unconstrained numeric so the exchange's scale is preserved.
-- Symbols are uppercase trading pairs, e.g. BTCUSDT.

create table portfolio (
    id         uuid         primary key,
    owner      varchar(100) not null,
    name       varchar(200) not null,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    version    bigint       not null default 0
);

create index portfolio_owner_idx on portfolio (owner);

create table holding (
    id           uuid        primary key,
    portfolio_id uuid        not null references portfolio (id) on delete cascade,
    symbol       varchar(20) not null check (symbol = upper(symbol)),
    quantity     numeric     not null check (quantity > 0),
    version      bigint      not null default 0,
    constraint holding_portfolio_symbol_uk unique (portfolio_id, symbol)
);

create table alert_rule (
    id               uuid        primary key,
    portfolio_id     uuid        not null references portfolio (id) on delete cascade,
    symbol           varchar(20) not null check (symbol = upper(symbol)),
    type             varchar(32) not null check (type in ('PRICE_ABOVE', 'PRICE_BELOW', 'PERCENT_CHANGE')),
    threshold        numeric,
    percent          numeric,
    window_seconds   integer,
    cooldown_seconds integer     not null default 60 check (cooldown_seconds >= 0),
    enabled          boolean     not null default true,
    created_at       timestamptz not null,
    updated_at       timestamptz not null,
    version          bigint      not null default 0,
    constraint alert_rule_params_ck check (
        (type in ('PRICE_ABOVE', 'PRICE_BELOW')
            and threshold is not null and percent is null and window_seconds is null)
        or (type = 'PERCENT_CHANGE'
            and threshold is null and percent > 0 and window_seconds > 0)
    )
);

create index alert_rule_portfolio_idx on alert_rule (portfolio_id);

-- Fired alerts (history). event_id makes consumer writes idempotent.
create table alert (
    id           uuid        primary key,
    event_id     uuid        not null unique,
    rule_id      uuid        references alert_rule (id) on delete set null,
    portfolio_id uuid        not null references portfolio (id) on delete cascade,
    symbol       varchar(20) not null check (symbol = upper(symbol)),
    price        numeric     not null,
    triggered_at timestamptz not null
);

create index alert_portfolio_triggered_idx on alert (portfolio_id, triggered_at desc);

-- Closed 1-minute candles. The primary key makes the persister's upsert idempotent.
create table candle (
    symbol      varchar(20) not null check (symbol = upper(symbol)),
    open_time   timestamptz not null,
    close_time  timestamptz not null,
    open        numeric     not null,
    high        numeric     not null,
    low         numeric     not null,
    close       numeric     not null,
    volume      numeric     not null check (volume >= 0),
    trade_count bigint      not null check (trade_count >= 0),
    primary key (symbol, open_time),
    constraint candle_time_ck check (close_time > open_time),
    constraint candle_ohlc_ck check (low <= open and low <= close and high >= open and high >= close)
);

-- Transactional outbox (ADR-0005). id is the event's eventId.
create table outbox (
    id             uuid         primary key,
    aggregate_type varchar(50)  not null,
    aggregate_id   uuid         not null,
    event_type     varchar(100) not null,
    payload        jsonb        not null,
    occurred_at    timestamptz  not null,
    published_at   timestamptz
);

create index outbox_unpublished_idx on outbox (occurred_at) where published_at is null;
