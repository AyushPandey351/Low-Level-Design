package ring;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// One worker per partition (see PartitionedNotificationQueue) - each worker owns
// exactly one queue and processes it single-threadedly, which is what makes FIFO
// order within that partition a guarantee rather than a hope. "Workers scale
// horizontally," per the notes - adding more partitions/workers increases
// cross-doorbell parallelism without touching NotificationService at all.
//
// Depends on EventSubscriber<DoorbellEvent> rather than the concrete
// NotificationService - NotificationService.process(DoorbellEvent) satisfies that
// interface via a method reference, and it lets Main's ordering test plug in a
// bare recording lambda instead, without NotificationWorker needing to know
// NotificationService exists at all.
public class NotificationWorker implements Runnable {
    private final BlockingQueue<DoorbellEvent> partition;
    private final EventSubscriber<DoorbellEvent> handler;
    private volatile boolean running = true;

    public NotificationWorker(BlockingQueue<DoorbellEvent> partition, EventSubscriber<DoorbellEvent> handler) {
        this.partition = partition;
        this.handler = handler;
    }

    @Override
    public void run() {
        while (running) {
            try {
                DoorbellEvent event = partition.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) {
                    handler.onEvent(event);
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
