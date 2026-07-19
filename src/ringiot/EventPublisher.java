package ringiot;

// Strategy/DIP: DeviceIngestionService depends on this INTERFACE, never on
// KafkaPublisher directly - swapping to a KinesisEventPublisher later (per the
// "what if Kafka is temporarily unavailable" follow-up's managed-alternative option)
// is a one-class change, zero changes to DeviceIngestionService.
public interface EventPublisher {
    void publish(Event event);
}
