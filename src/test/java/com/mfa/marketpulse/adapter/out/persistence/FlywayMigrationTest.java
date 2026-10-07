package com.mfa.marketpulse.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.pgclient.PgException;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** V1 creates every table from architecture.md section 6, and its constraints reject invalid rows. */
@QuarkusTest
class FlywayMigrationTest {

    @Inject
    Pool pool;

    @Test
    void createsAllTables() {
        List<String> tables =
                query("select table_name from information_schema.tables where table_schema = 'public'").stream()
                        .map(row -> row.getString("table_name"))
                        .toList();

        assertThat(tables).contains("portfolio", "holding", "alert_rule", "alert", "candle", "outbox");
    }

    @Test
    void rejectsLowercaseSymbolAndNonPositiveQuantity() {
        UUID portfolio = insertPortfolio();

        assertViolates("insert into holding (id, portfolio_id, symbol, quantity) values (gen_random_uuid(), '"
                + portfolio + "', 'btcusdt', 1)");
        assertViolates("insert into holding (id, portfolio_id, symbol, quantity) values (gen_random_uuid(), '"
                + portfolio + "', 'BTCUSDT', 0)");
    }

    @Test
    void rejectsSecondHoldingForSameSymbol() {
        UUID portfolio = insertPortfolio();
        String insert = "insert into holding (id, portfolio_id, symbol, quantity) values (gen_random_uuid(), '"
                + portfolio + "', 'ETHUSDT', 2)";
        query(insert);

        assertViolates(insert);
    }

    @Test
    void rejectsAlertRuleWithParametersOfWrongType() {
        UUID portfolio = insertPortfolio();

        assertViolates("insert into alert_rule (id, portfolio_id, symbol, type, percent, window_seconds,"
                + " created_at, updated_at) values (gen_random_uuid(), '" + portfolio
                + "', 'BTCUSDT', 'PRICE_ABOVE', 5, 600, now(), now())");
    }

    @Test
    void rejectsCandleWithLowAboveHigh() {
        assertViolates("insert into candle (symbol, open_time, close_time, open, high, low, close, volume,"
                + " trade_count) values ('BTCUSDT', '2026-10-07T10:00:00Z', '2026-10-07T10:01:00Z',"
                + " 100, 90, 110, 100, 1, 1)");
    }

    @Test
    void deletingPortfolioCascadesToHoldings() {
        UUID portfolio = insertPortfolio();
        query("insert into holding (id, portfolio_id, symbol, quantity) values (gen_random_uuid(), '" + portfolio
                + "', 'SOLUSDT', 3)");

        query("delete from portfolio where id = '" + portfolio + "'");

        assertThat(query("select 1 from holding where portfolio_id = '" + portfolio + "'"))
                .isEmpty();
    }

    private UUID insertPortfolio() {
        UUID id = UUID.randomUUID();
        query("insert into portfolio (id, owner, name, created_at, updated_at) values ('" + id
                + "', 'demo', 'Test', now(), now())");
        return id;
    }

    private void assertViolates(String sql) {
        assertThatThrownBy(() -> query(sql))
                .isInstanceOf(PgException.class)
                // 23xxx = integrity constraint violation (check, unique, not null, foreign key)
                .satisfies(e -> assertThat(((PgException) e).getSqlState()).startsWith("23"));
    }

    private List<Row> query(String sql) {
        var rows = pool.query(sql).execute().await().indefinitely();
        return StreamSupport.stream(rows.spliterator(), false).toList();
    }
}
