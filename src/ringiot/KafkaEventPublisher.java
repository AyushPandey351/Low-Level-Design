package ringiot;

public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTopic topic;

    public KafkaEventPublisher(KafkaTopic topic) {
        this.topic = topic;
    }

    @Override
    public void publish(Event event) {
        topic.publish(event);
    }
}
