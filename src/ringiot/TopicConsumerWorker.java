package ringiot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// One worker per partition per consumer group - same "single-threaded consumer per
// partition guarantees FIFO" discipline as NotificationWorker in the Ring
// Notification Service design. Depends on the generic EventHandler functional
// interface rather than any concrete processor, so Main can wire completely
// different behavior into the Notification/Storage/Analytics consumer groups
// without this class knowing any of them exist.
public class TopicConsumerWorker implements Runnable {
    private final BlockingQueue<Event> partition;
    private final EventHandler handler;
    private volatile boolean running = true;

    public TopicConsumerWorker(BlockingQueue<Event> partition, EventHandler handler) {
        this.partition = partition;
        this.handler = handler;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Event event = partition.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) {
                    handler.handle(event);
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
