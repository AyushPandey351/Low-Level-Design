package ring;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Simulates "SQS FIFO queues with MessageGroupId = doorbellId" / "Kafka partitions
// keyed by doorbellId" from the notes - concretely, not just as a comment. Every
// DoorbellEvent for the SAME doorbellId hashes to the SAME partition, and each
// partition is drained by exactly ONE NotificationWorker thread - a single-threaded
// consumer can only ever process its own queue in strict FIFO order, which is what
// actually GUARANTEES per-doorbell ordering (motion -> ring -> ring stays in that
// order for one doorbell). Different doorbells usually land in different
// partitions, so they're processed in PARALLEL by different worker threads - "this
// preserves ordering for a single home while allowing parallelism across homes,"
// exactly as the notes describe.
public class PartitionedNotificationQueue {
    private final BlockingQueue<DoorbellEvent>[] partitions;
    private final int partitionCount;

    @SuppressWarnings("unchecked")
    public PartitionedNotificationQueue(int partitionCount) {
        this.partitionCount = partitionCount;
        this.partitions = new BlockingQueue[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            partitions[i] = new LinkedBlockingQueue<>();
        }
    }

    public void enqueue(DoorbellEvent event) {
        partitions[partitionFor(event.getDoorbellId())].add(event);
    }

    public BlockingQueue<DoorbellEvent> getPartition(int index) {
        return partitions[index];
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    private int partitionFor(String doorbellId) {
        return Math.floorMod(doorbellId.hashCode(), partitionCount);
    }
}
