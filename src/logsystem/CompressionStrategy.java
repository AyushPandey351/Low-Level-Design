package logsystem;

// Strategy Pattern - the agent depends on this interface, never on a concrete
// algorithm directly, matching "agents compress logs (gzip/snappy/zstd)" from the
// notes. Returns a simulated compressed size rather than actually compressing bytes
// (no real gzip/snappy library call, same "ignore real implementation" treatment as
// every external algorithm in this series) - what's being demonstrated is that
// swapping the algorithm changes the bandwidth-savings number without touching
// LogAgent at all.
public interface CompressionStrategy {
    int compress(int originalSizeBytes);

    String getName();
}
