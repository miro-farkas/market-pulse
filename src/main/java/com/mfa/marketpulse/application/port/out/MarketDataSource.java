package com.mfa.marketpulse.application.port.out;

import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.Multi;
import java.util.Set;

/** A live, push-based source of trades (exchange or simulator). */
public interface MarketDataSource {

    /**
     * Hot stream of ticks for the given symbols. Never completes on its own; implementations reconnect on failure.
     * Cancelling the subscription closes the underlying connection.
     */
    Multi<Tick> ticks(Set<Symbol> symbols);
}
