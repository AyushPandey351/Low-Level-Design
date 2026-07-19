package ringiot;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

// THE structural answer to "why Kafka instead of SQS" - not a comment, actual
// different behavior. In the Ring Notification Service design, PartitionedNotificationQueue
// modeled SQS: one event landed in ONE partition, consumed by competing workers - a
// single copy, first worker to grab it wins. THIS class models Kafka's real
// semantics: publish() gives EVERY registered consumer group its OWN independent
// copy of the event, each in that group's own partition-by-deviceId queue set.
// Notification Processor, Analytics Processor, and Storage Processor each see every
// single event - they are NOT competing for the same messages, they're three
// entirely separate readers of the same log. That's "multiple independent consumer
// groups" from the notes' comparison table, made real.
//
// Ordering is still per-device WITHIN each consumer group's own partition set,
// exactly as before - the fan-out to multiple groups and the ordering guarantee are
// two independent properties, both provided by the same partition-by-deviceId scheme.
public class KafkaTopic {
    private final int partitionCount;
    private final Map<String, BlockingQueue<Event>[]> consumerGroups = new ConcurrentHashMap<>();

    public KafkaTopic(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    @SuppressWarnings("unchecked")
    public void registerConsumerGroup(String groupName) {
        BlockingQueue<Event>[] partitions = new BlockingQueue[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            partitions[i] = new LinkedBlockingQueue<>();
        }
        consumerGroups.put(groupName, partitions);
    }

    public void publish(Event event) {
        int partitionIndex = partitionFor(event.getDeviceId());
        for (BlockingQueue<Event>[] partitions : consumerGroups.values()) {
            partitions[partitionIndex].add(event); // each consumer group gets its OWN copy
        }
    }

    public BlockingQueue<Event> getPartition(String groupName, int index) {
        return consumerGroups.get(groupName)[index];
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    public int sizeOf(String groupName) {
        int total = 0;
        for (BlockingQueue<Event> partition : consumerGroups.get(groupName)) {
            total += partition.size();
        }
        return total;
    }

    private int partitionFor(String deviceId) {
        return Math.floorMod(deviceId.hashCode(), partitionCount);
    }
}
