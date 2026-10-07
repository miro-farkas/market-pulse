package com.mfa.marketpulse.adapter.out.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Binance {@code <symbol>@trade} stream payload. Binance sends prices and quantities as strings.
 *
 * @param tradeTime trade time in epoch milliseconds ({@code T})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record BinanceTradeMessage(
        @JsonProperty("e") String eventType,
        @JsonProperty("s") String symbol,
        @JsonProperty("p") BigDecimal price,
        @JsonProperty("q") BigDecimal quantity,
        @JsonProperty("T") long tradeTime) {

    static final String TRADE = "trade";
}
