package notification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Schedules a future notification send using a ScheduledExecutorService - a real
// production system would use a durable scheduled-job store (so scheduled sends
// survive a process restart), but the mechanics of "run this later" are the same
// idea. cancel() returns the ScheduledFuture handle from schedule() specifically so
// a caller CAN actually cancel it before it fires - a fire-and-forget schedule()
// with no return value would make the notes' cancel() API impossible to implement.
public class Scheduler {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    public ScheduledFuture<?> schedule(Runnable task, LocalDateTime scheduledTime) {
        long delayMillis = Duration.between(LocalDateTime.now(), scheduledTime).toMillis();
        return executor.schedule(task, Math.max(delayMillis, 0), TimeUnit.MILLISECONDS);
    }

    public void cancel(ScheduledFuture<?> future) {
        future.cancel(false);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
