package com.mfa.marketpulse.adapter.out.exchange;

/** The exchange WebSocket closed or failed. Triggers a reconnect with backoff. */
public class ExchangeConnectionLostException extends RuntimeException {

    public ExchangeConnectionLostException(String message) {
        super(message);
    }
}
