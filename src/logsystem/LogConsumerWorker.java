package logsystem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// One worker per partition per consumer group, same single-threaded-per-partition
// discipline used throughout this series to make FIFO-within-a-partition a real
// guarantee. Generic over a plain Consumer<LogEntry> so completely different logic
// (indexing, alerting, archiving, live-tail fan-out) can be wired in without this
// class knowing any of those concerns exist.
public class LogConsumerWorker implements Runnable {
    private final BlockingQueue<LogEntry> partition;
    private final Consumer<LogEntry> handler;
    private volatile boolean running = true;

    public LogConsumerWorker(BlockingQueue<LogEntry> partition, Consumer<LogEntry> handler) {
        this.partition = partition;
        this.handler = handler;
    }

    @Override
    public void run() {
        while (running) {
            try {
                LogEntry log = partition.poll(200, TimeUnit.MILLISECONDS);
                if (log != null) {
                    handler.accept(log);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
