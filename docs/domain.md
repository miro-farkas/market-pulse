# Domain

## Glossary

| Term | Meaning |
|---|---|
| Symbol | Trading pair, e.g. `BTCUSDT`. Uppercase, validated against configured symbols. |
| Tick | Single trade/price update from the exchange: symbol, price, quantity, tradeTime. |
| Candle | OHLCV aggregate for one symbol and one fixed window (1 minute): open, high, low, close, volume, openTime, closeTime, tradeCount. |
| Portfolio | Named set of holdings owned by a user. |
| Holding | Symbol + quantity inside a portfolio (quantity > 0). |
| Valuation | Portfolio value at a point in time: sum(quantity × last price) per holding + total, in quote currency (USDT). |
| AlertRule | Condition on a symbol owned by a portfolio, evaluated against ticks. |
| Alert | Fired instance of a rule with the triggering price and time. |

## Value rules
- Prices, quantities, values: `BigDecimal`. Prices keep exchange scale; computed values use scale 8,
  `RoundingMode.HALF_EVEN`.
- Time: `Instant`, UTC. Candle windows aligned to the minute (`openTime` truncated to minutes).
- Exchange event time is authoritative for candles, not processing time. Late ticks (older than the
  currently open window) are ignored and counted in a metric.

## Alert rule types (v1)
| Type | Parameters | Fires when |
|---|---|---|
| `PRICE_ABOVE` | threshold | price crosses above threshold (previous ≤ threshold < current) |
| `PRICE_BELOW` | threshold | price crosses below threshold |
| `PERCENT_CHANGE` | percent, window (e.g. 10 min) | abs change within the window ≥ percent |

- Crossing semantics (not level): a rule fires once per crossing, not on every tick above the level.
- Cooldown per rule (default 60 s) after firing.
- Disabled rules are never evaluated.

## Invariants
- A portfolio holds at most one holding per symbol.
- Holdings and rules may only reference configured symbols.
- Deleting a portfolio deletes its holdings and rules (and emits change events).
- Candle invariant: low ≤ open, close ≤ high; volume ≥ 0.
