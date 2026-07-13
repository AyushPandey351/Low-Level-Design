package notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

// Singleton ("one shared service," per your notes) and the main coordinator - same
// initialize()/getInstance() split used throughout this series, since it needs
// configured collaborators (factory, retry service, repository, scheduler) at
// creation time.
//
// sendNotification() is Step 8's "Solution 1: Thread Pool" made concrete: each
// requested channel's delivery is submitted to `executor` and runs asynchronously,
// so fanning a message out across Email+SMS+Push (or blasting it to 1000 users)
// doesn't block waiting for each send in turn - exactly the "very slow" problem your
// notes describe with the naive loop.
//
// Also implements EventListener<OrderPlacedEvent> - THIS is the Observer/Pub-Sub tie-in:
// NotificationService subscribes itself to an OrderService's publisher (see Main),
// and onEvent() reacts by fanning out a notification. OrderService never references
// NotificationService at all.
public class NotificationService implements EventListener<OrderPlacedEvent> {
    private static NotificationService instance;

    private final NotificationFactory factory;
    private final RetryService retryService;
    private final NotificationRepository repository;
    private final Scheduler scheduler;
    private final ExecutorService executor;
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger();

    private NotificationService(NotificationFactory factory, RetryService retryService,
                                 NotificationRepository repository, Scheduler scheduler, int threadPoolSize) {
        this.factory = factory;
        this.retryService = retryService;
        this.repository = repository;
        this.scheduler = scheduler;
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    public static synchronized NotificationService initialize(NotificationFactory factory, RetryService retryService,
                                                                NotificationRepository repository, Scheduler scheduler,
                                                                int threadPoolSize) {
        if (instance != null) {
            throw new IllegalStateException("NotificationService is already initialized");
        }
        instance = new NotificationService(factory, retryService, repository, scheduler, threadPoolSize);
        return instance;
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NotificationService has not been initialized");
        }
        return instance;
    }

    public void registerUser(User user) {
        users.put(user.getUserId(), user);
    }

    // Fans out into one Notification PER requested channel the user actually
    // permits (see Notification's comment on this interpretation), each dispatched
    // asynchronously via the thread pool.
    public List<Notification> sendNotification(String userId, String title, String message,
                                                 List<ChannelType> requestedChannels) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + userId);
        }

        List<Notification> created = new ArrayList<>();
        for (ChannelType channelType : requestedChannels) {
            if (!user.getPreference().allowsChannel(channelType)) {
                System.out.println("[NotificationService] Skipping " + channelType + " for " + user.getName()
                        + " - not in their preferred channels");
                continue;
            }
            String id = "NOTIF" + idCounter.incrementAndGet();
            Notification notification = new Notification(id, title, message, user, channelType);
            repository.save(notification);
            created.add(notification);

            NotificationChannel channel = factory.getChannel(channelType);
            executor.submit(() -> dispatch(notification, channel));
        }
        return created;
    }

    public ScheduledFuture<?> scheduleNotification(String userId, String title, String message,
                                                     List<ChannelType> channels, LocalDateTime scheduledTime) {
        return scheduler.schedule(() -> sendNotification(userId, title, message, channels), scheduledTime);
    }

    // An EXPLICIT, caller-requested retry of an already-FAILED notification - this
    // deliberately does NOT go through markProcessedIfNew(). That guard exists to
    // protect against an ACCIDENTAL duplicate delivery of the same job (e.g. a
    // crashed worker's task getting redelivered by an at-least-once queue) - it is
    // not meant to block a legitimate, intentional "please try this again" request,
    // which is exactly what this API represents.
    public void retry(String notificationId) {
        Notification notification = repository.find(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("No such notification: " + notificationId);
        }
        NotificationChannel channel = factory.getChannel(notification.getChannel());
        executor.submit(() -> {
            retryService.sendWithRetry(notification, channel);
            repository.update(notification);
        });
    }

    public void updatePreference(String userId, NotificationPreference preference) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + userId);
        }
        user.updatePreference(preference);
    }

    public NotificationStatus getNotificationStatus(String notificationId) {
        Notification notification = repository.find(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("No such notification: " + notificationId);
        }
        return notification.getStatus();
    }

    @Override
    public void onEvent(OrderPlacedEvent event) {
        sendNotification(event.getUserId(), "Order Confirmed",
                "Your order " + event.getOrderId() + " for " + event.getAmount() + " has been placed.",
                List.of(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH));
    }

    // This IS the idempotency guard (Step 8, Solution 3) - protects against the same
    // async dispatch task somehow running twice for the same notificationId (e.g. a
    // redelivered job in a real queue-backed worker), not against legitimate retries
    // triggered via retry() above.
    private void dispatch(Notification notification, NotificationChannel channel) {
        if (!repository.markProcessedIfNew(notification.getNotificationId())) {
            System.out.println("[NotificationService] Skipping duplicate processing of " + notification.getNotificationId());
            return;
        }
        retryService.sendWithRetry(notification, channel);
        repository.update(notification);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
