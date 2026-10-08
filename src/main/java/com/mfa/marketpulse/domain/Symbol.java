package com.mfa.marketpulse.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Trading pair such as {@code BTCUSDT}. Always uppercase letters and digits. */
public record Symbol(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]+");

    public Symbol {
        Objects.requireNonNull(value, "symbol");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Symbol must be uppercase letters and digits: '" + value + "'");
        }
    }

    /** Accepts any case, e.g. the lowercase form exchanges use in stream names. */
    public static Symbol of(String value) {
        Objects.requireNonNull(value, "symbol");
        return new Symbol(value.trim().toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
