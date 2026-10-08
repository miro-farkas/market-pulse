package com.mfa.marketpulse.application.port.in;

/** Continuously moves ticks from the market data source to the tick topic. */
public interface IngestTicksUseCase {

    /** Starts ingestion. Calling it while running has no effect. */
    void start();

    /** Stops ingestion and releases the source connection. */
    void stop();

    /** Counters since start, for health and metrics. */
    Stats stats();

    record Stats(boolean running, long published, long dropped, long failed) {}
}
