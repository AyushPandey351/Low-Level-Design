package notification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        NotificationFactory factory = new NotificationFactory();
        RetryService retryService = new RetryService();
        NotificationRepository repository = new NotificationRepository();
        Scheduler scheduler = new Scheduler();
        NotificationService service = NotificationService.initialize(factory, retryService, repository, scheduler, 8);

        User ayush = new User("U1", "Ayush", "ayush@example.com", "+91-1111111111", "device-token-1",
                new NotificationPreference(Arrays.asList(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH), true, true));
        User rahul = new User("U2", "Rahul", "rahul@example.com", "+91-2222222222", "device-token-2",
                new NotificationPreference(List.of(ChannelType.EMAIL), true, true)); // only wants email
        service.registerUser(ayush);
        service.registerUser(rahul);

        // --- Basic send, respecting per-user channel preferences ---
        System.out.println("== Ayush (prefers Email+SMS+Push) gets a 3-channel notification ==");
        List<Notification> ayushNotifs = service.sendNotification(
                ayush.getUserId(), "Welcome!", "Thanks for joining.", Arrays.asList(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH));
        Thread.sleep(200); // let the async dispatches finish for this demo's deterministic output
        for (Notification n : ayushNotifs) {
            System.out.println(n.getNotificationId() + " (" + n.getChannel() + "): " + service.getNotificationStatus(n.getNotificationId()));
        }

        System.out.println("\n== Rahul (prefers Email only) gets the SAME 3-channel request ==");
        List<Notification> rahulNotifs = service.sendNotification(
                rahul.getUserId(), "Welcome!", "Thanks for joining.", Arrays.asList(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH));
        Thread.sleep(200);
        System.out.println("Notifications actually created for Rahul: " + rahulNotifs.size() + " (SMS/Push were skipped)");

        // --- Retry with exponential backoff: email fails twice, succeeds on 3rd attempt ---
        System.out.println("\n== Flaky email provider: fails twice, then succeeds ==");
        factory.registerChannel(ChannelType.EMAIL, new FlakyChannelWrapper(new EmailNotificationChannel(), 2));
        List<Notification> flakyNotifs = service.sendNotification(
                ayush.getUserId(), "Flash Sale", "50% off today only!", List.of(ChannelType.EMAIL));
        Thread.sleep(1500); // must exceed 200ms + 400ms backoff plus send time
        System.out.println("Final status: " + service.getNotificationStatus(flakyNotifs.get(0).getNotificationId()));

        // --- Permanent failure, then a manual retry() once the provider is fixed ---
        System.out.println("\n== SMS provider down entirely: exhausts all retries and FAILS ==");
        factory.registerChannel(ChannelType.SMS, new FlakyChannelWrapper(new SMSNotificationChannel(), 100));
        List<Notification> deadNotifs = service.sendNotification(
                ayush.getUserId(), "OTP", "Your OTP is 482913", List.of(ChannelType.SMS));
        Thread.sleep(1800); // 200 + 400 + 800ms backoff across 3 retries, plus margin
        String deadId = deadNotifs.get(0).getNotificationId();
        System.out.println("Status after exhausting retries: " + service.getNotificationStatus(deadId));

        System.out.println("Provider is fixed - registering a working SMS channel and retrying manually...");
        factory.registerChannel(ChannelType.SMS, new SMSNotificationChannel());
        service.retry(deadId);
        Thread.sleep(200);
        System.out.println("Status after manual retry(): " + service.getNotificationStatus(deadId));

        // --- Idempotency guard: same id can only be "claimed" for processing once ---
        System.out.println("\n== Idempotency: markProcessedIfNew on the same id twice ==");
        System.out.println("First claim: " + repository.markProcessedIfNew("DUPLICATE_TEST_ID"));
        System.out.println("Second claim (must be false - already processed): " + repository.markProcessedIfNew("DUPLICATE_TEST_ID"));

        // --- Observer/Pub-Sub: OrderService publishes, NotificationService reacts, with zero coupling ---
        System.out.println("\n== Observer/Pub-Sub: OrderService knows nothing about NotificationService ==");
        OrderService orderService = new OrderService();
        orderService.subscribe(service);
        orderService.placeOrder(ayush.getUserId(), "ORDER123", 2499.0);
        Thread.sleep(200);

        // --- Scheduled notification ---
        System.out.println("\n== Scheduling a notification 500ms in the future ==");
        service.scheduleNotification(ayush.getUserId(), "Reminder", "Your cart is waiting!",
                List.of(ChannelType.EMAIL), LocalDateTime.now().plusNanos(500_000_000));
        System.out.println("Scheduled - nothing should print yet...");
        Thread.sleep(700);
        System.out.println("...and now it should have fired above.");

        // --- Real concurrency test: 100 different users, all notified simultaneously ---
        System.out.println("\n== Concurrency: 100 users registered and notified simultaneously (the '1000 users' scenario) ==");
        int userCount = 100;
        for (int i = 0; i < userCount; i++) {
            String id = "BULK_U" + i;
            service.registerUser(new User(id, "User" + i, id + "@example.com", "+91-000000" + i, "device-" + i,
                    new NotificationPreference(List.of(ChannelType.EMAIL), true, true)));
        }
        factory.registerChannel(ChannelType.EMAIL, new EmailNotificationChannel()); // reset to the reliable channel

        ExecutorService loadGenerator = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(userCount);
        AtomicInteger createdCount = new AtomicInteger();

        for (int i = 0; i < userCount; i++) {
            String id = "BULK_U" + i;
            loadGenerator.submit(() -> {
                awaitLatch(startLatch);
                List<Notification> created = service.sendNotification(id, "Promotion", "50% off everything!", List.of(ChannelType.EMAIL));
                createdCount.addAndGet(created.size());
                doneLatch.countDown();
            });
        }
        startLatch.countDown();
        doneLatch.await();
        loadGenerator.shutdown();
        Thread.sleep(500); // let the async sends finish

        System.out.println("Notifications created: " + createdCount.get() + " (must be exactly " + userCount + ")");

        service.shutdown();
        scheduler.shutdown();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
