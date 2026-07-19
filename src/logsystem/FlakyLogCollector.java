package logsystem;

import java.util.concurrent.atomic.AtomicInteger;

// Test-only decorator - fails the first N sends before delegating through, purely
// so Main can deterministically exercise LogAgent's retry logic.
public class FlakyLogCollector implements LogCollector {
    private final LogCollector delegate;
    private final AtomicInteger failuresRemaining;

    public FlakyLogCollector(LogCollector delegate, int failuresBeforeSuccess) {
        this.delegate = delegate;
        this.failuresRemaining = new AtomicInteger(failuresBeforeSuccess);
    }

    @Override
    public void collect(LogEntry log) {
        if (failuresRemaining.getAndDecrement() > 0) {
            throw new RuntimeException("Simulated transient send failure");
        }
        delegate.collect(log);
    }
}
