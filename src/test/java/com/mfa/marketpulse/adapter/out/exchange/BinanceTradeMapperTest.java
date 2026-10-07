package com.mfa.marketpulse.adapter.out.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mapping of Binance frames, using samples in {@code src/test/resources/binance}. */
class BinanceTradeMapperTest {

    private final BinanceTradeMapper mapper = new BinanceTradeMapper(new ObjectMapper());

    @Test
    void mapsTradeKeepingExchangeScale() {
        assertThat(mapper.map(sample("trade.json")))
                .contains(new Tick(
                        new Symbol("BTCUSDT"),
                        new BigDecimal("62012.34000000"),
                        new BigDecimal("0.00150000"),
                        Instant.ofEpochMilli(1759831200120L)));
    }

    @Test
    void ignoresSubscriptionReply() {
        assertThat(mapper.map(sample("subscribe-response.json"))).isEmpty();
    }

    @Test
    void ignoresOtherEventTypes() {
        assertThat(mapper.map(sample("agg-trade.json"))).isEmpty();
    }

    @Test
    void skipsTradeThatViolatesTickRules() {
        assertThat(mapper.map(sample("trade-zero-quantity.json"))).isEmpty();
    }

    @Test
    void skipsMalformedFrames() {
        assertThat(mapper.map("{not json")).isEmpty();
        assertThat(mapper.map("{\"e\":\"trade\",\"s\":\"BTCUSDT\"}")).isEmpty();
        assertThat(mapper.map("{\"e\":\"trade\",\"s\":\"BTCUSDT\",\"p\":\"abc\",\"q\":\"1\",\"T\":1}"))
                .isEmpty();
    }

    @Test
    void subscribeMessageUsesLowercaseTradeStreams() {
        assertThat(BinanceTradeMapper.subscribeMessage(List.of(new Symbol("BTCUSDT"), new Symbol("ETHUSDT")), 7))
                .isEqualTo("{\"method\":\"SUBSCRIBE\",\"params\":[\"btcusdt@trade\",\"ethusdt@trade\"],\"id\":7}");
    }

    private static String sample(String name) {
        try (InputStream in = BinanceTradeMapperTest.class.getResourceAsStream("/binance/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
