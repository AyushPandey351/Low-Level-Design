package ring;

import java.util.List;

public interface RetryService {
    void scheduleRetry(Notification notification, Device device, PushProvider provider);

    void moveToDLQ(Notification notification);

    List<Notification> getDeadLetterQueue();
}
