package logsystem;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

// The third time this exact structure appears in this repo (after the Ring IoT
// Event Processing and Ring Video designs) - Kafka's real distinguishing behavior
// vs. a plain queue: publish() gives EVERY registered consumer group (indexer,
// alert, archive, live-tail, analytics) its OWN independent copy of the stream,
// each partitioned by serviceName so per-service ordering holds within each group
// without limiting cross-service parallelism. Same reasoning as before, not
// repeated at length here - see KafkaTopic in the ringiot package for the full
// walkthrough of why this differs from a single-copy SQS-style queue.
public class LogTopic {
    private final int partitionCount;
    private final Map<String, BlockingQueue<LogEntry>[]> consumerGroups = new ConcurrentHashMap<>();

    public LogTopic(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    @SuppressWarnings("unchecked")
    public void registerConsumerGroup(String groupName) {
        BlockingQueue<LogEntry>[] partitions = new BlockingQueue[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            partitions[i] = new LinkedBlockingQueue<>();
        }
        consumerGroups.put(groupName, partitions);
    }

    public void publish(LogEntry log) {
        int partitionIndex = Math.floorMod(log.getServiceName().hashCode(), partitionCount);
        for (BlockingQueue<LogEntry>[] partitions : consumerGroups.values()) {
            partitions[partitionIndex].add(log);
        }
    }

    public BlockingQueue<LogEntry> getPartition(String groupName, int index) {
        return consumerGroups.get(groupName)[index];
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    public int sizeOf(String groupName) {
        int total = 0;
        for (BlockingQueue<LogEntry> partition : consumerGroups.get(groupName)) {
            total += partition.size();
        }
        return total;
    }
}
