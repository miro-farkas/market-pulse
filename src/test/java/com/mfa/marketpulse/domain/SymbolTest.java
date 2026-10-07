package com.mfa.marketpulse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SymbolTest {

    @Test
    void acceptsUppercasePair() {
        assertThat(new Symbol("BTCUSDT").value()).isEqualTo("BTCUSDT");
    }

    @Test
    void ofNormalizesCaseAndWhitespace() {
        assertThat(Symbol.of(" btcusdt ")).isEqualTo(new Symbol("BTCUSDT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "btcusdt", "BTC-USDT", "BTC USDT"})
    void rejectsInvalidFormat(String value) {
        assertThatThrownBy(() -> new Symbol(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Symbol(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Symbol.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringIsTheValue() {
        assertThat(new Symbol("ETHUSDT")).hasToString("ETHUSDT");
    }
}
