package logsystem;

public class KafkaLogCollector implements LogCollector {
    private final LogTopic topic;

    public KafkaLogCollector(LogTopic topic) {
        this.topic = topic;
    }

    @Override
    public void collect(LogEntry log) {
        topic.publish(log);
    }
}
