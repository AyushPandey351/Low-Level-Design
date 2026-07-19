package ring;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        DeviceRepository deviceRepository = new InMemoryDeviceRepository();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        PushProviderFactory providerFactory = new PushProviderFactory();
        RetryService retryService = new ExponentialBackoffRetryService(notificationRepository);
        IdempotencyService idempotencyService = new RedisIdempotencyService(Duration.ofSeconds(5));
        Debouncer debouncer = new Debouncer(Duration.ofMillis(500));
        NotificationService notificationService = new NotificationService(
                deviceRepository, providerFactory, notificationRepository, retryService, idempotencyService, debouncer);

        // --- Home9: Ayush's home, matching the notes' example (iPhone, iPad, Pixel, Alexa) ---
        deviceRepository.save(new Device("D1", "ayush", "home9", "ayush-iphone-token", Platform.IOS));
        deviceRepository.save(new Device("D2", "ayush", "home9", "ayush-ipad-token", Platform.IOS));
        deviceRepository.save(new Device("D3", "ayush", "home9", "ayush-pixel-token", Platform.ANDROID));
        deviceRepository.save(new Device("D4", "ayush", "home9", "ayush-echo-token", Platform.ALEXA));

        // --- SQS-equivalent: partitioned by doorbellId so ordering is per-doorbell ---
        PartitionedNotificationQueue queue = new PartitionedNotificationQueue(4);
        List<NotificationWorker> workers = new ArrayList<>();
        List<Thread> workerThreads = new ArrayList<>();
        for (int i = 0; i < queue.getPartitionCount(); i++) {
            NotificationWorker worker = new NotificationWorker(queue.getPartition(i), notificationService::process);
            workers.add(worker);
            Thread t = new Thread(worker, "worker-" + i);
            t.start();
            workerThreads.add(t);
        }

        // --- SNS-equivalent: one event, three independent, decoupled consumers ---
        EventBus<DoorbellEvent> eventBus = new EventBus<>();
        eventBus.subscribe(queue::enqueue); // notification fan-out target
        eventBus.subscribe(event -> System.out.println("[Analytics] logged doorbell press " + event.getEventId()));
        eventBus.subscribe(event -> System.out.println("[Video] started recording clip for " + event.getDoorbellId()));

        // --- Basic flow: press -> fan-out -> devices notified ---
        System.out.println("== Doorbell press: home9/doorbell12 ==");
        eventBus.publish(new DoorbellEvent("evt1", "home9", "doorbell12", Instant.now()));
        Thread.sleep(300);

        // --- Debounce: a second, DIFFERENT press on the SAME doorbell, too soon ---
        System.out.println("\n== Immediate second press (different eventId) on the same doorbell ==");
        eventBus.publish(new DoorbellEvent("evt2", "home9", "doorbell12", Instant.now()));
        Thread.sleep(300);

        // --- Idempotency: after the debounce window passes, redeliver the ORIGINAL eventId ---
        System.out.println("\n== After debounce window: SAME eventId (evt1) redelivered (e.g. SQS at-least-once) ==");
        Thread.sleep(300); // total > 500ms debounce window since evt1
        eventBus.publish(new DoorbellEvent("evt1", "home9", "doorbell12", Instant.now()));
        // The redelivery above still PASSED the debounce check before idempotency caught
        // it (debounce and idempotency are independent layers - see NotificationService's
        // comment), which refreshed the debounce timestamp for this doorbell. Wait past
        // the debounce window again before the next distinct press, or it would get
        // debounced instead of cleanly demonstrating retry behavior.
        Thread.sleep(600);

        // --- Retry with backoff: Pixel's FCM provider fails twice, then succeeds ---
        System.out.println("\n== FCM (Pixel) flaky: fails twice, then succeeds ==");
        providerFactory.registerProvider(Platform.ANDROID, new FlakyPushProvider(new FCMPushProvider(), 2));
        eventBus.publish(new DoorbellEvent("evt3", "home9", "doorbell12", Instant.now()));
        Thread.sleep(1000); // covers backoff of 200ms + 1000ms plus processing

        // --- DLQ: Alexa's provider is down entirely ---
        System.out.println("\n== Alexa provider down entirely: exhausts retries, lands in DLQ ==");
        providerFactory.registerProvider(Platform.ALEXA, new FlakyPushProvider(new AlexaPushProvider(), 100));
        eventBus.publish(new DoorbellEvent("evt4", "home9", "doorbell12", Instant.now()));
        Thread.sleep(5000); // 200 + 1000 + 3000ms across 3 backoff delays, plus margin, before it lands in the DLQ
        System.out.println("Dead letter queue size: " + retryService.getDeadLetterQueue().size());
        for (Notification n : retryService.getDeadLetterQueue()) {
            System.out.println("  " + n.getNotificationId() + " for device " + n.getDeviceId() + ": " + n.getStatus());
        }

        // --- Ordering proof, ISOLATED from debounce/idempotency: direct partition-level test ---
        System.out.println("\n== Ordering: 3 events for the SAME doorbellId, processed by a dedicated recorder ==");
        List<String> processingOrder = new CopyOnWriteArrayList<>();
        PartitionedNotificationQueue orderingQueue = new PartitionedNotificationQueue(4);
        NotificationWorker recorderWorker = new NotificationWorker(
                orderingQueue.getPartition(orderingPartitionIndex(orderingQueue, "orderTestDoorbell")),
                event -> processingOrder.add(event.getEventId()));
        Thread recorderThread = new Thread(recorderWorker, "order-recorder");
        recorderThread.start();
        orderingQueue.enqueue(new DoorbellEvent("order-e1", "homeX", "orderTestDoorbell", Instant.now()));
        orderingQueue.enqueue(new DoorbellEvent("order-e2", "homeX", "orderTestDoorbell", Instant.now()));
        orderingQueue.enqueue(new DoorbellEvent("order-e3", "homeX", "orderTestDoorbell", Instant.now()));
        Thread.sleep(300);
        recorderWorker.stop();
        recorderThread.join();
        System.out.println("Processing order: " + processingOrder + " (must be exactly [order-e1, order-e2, order-e3])");

        // --- Real concurrency test: many different homes pressed simultaneously ---
        System.out.println("\n== Concurrency: 50 different homes pressed simultaneously ==");
        int homeCount = 50;
        for (int i = 0; i < homeCount; i++) {
            String homeId = "bulkHome" + i;
            deviceRepository.save(new Device("BD" + i + "_1", "user" + i, homeId, "token" + i + "_1", Platform.WEB));
            deviceRepository.save(new Device("BD" + i + "_2", "user" + i, homeId, "token" + i + "_2", Platform.WEB));
        }

        ExecutorService loadGenerator = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(homeCount);
        for (int i = 0; i < homeCount; i++) {
            String homeId = "bulkHome" + i;
            String doorbellId = "bulkDoorbell" + i;
            loadGenerator.submit(() -> {
                awaitLatch(startLatch);
                eventBus.publish(new DoorbellEvent("bulk-evt-" + homeId, homeId, doorbellId, Instant.now()));
                doneLatch.countDown();
            });
        }
        startLatch.countDown();
        doneLatch.await();
        loadGenerator.shutdown();
        Thread.sleep(1000); // let the worker pool drain the burst

        int totalQueued = 0;
        for (int i = 0; i < queue.getPartitionCount(); i++) {
            totalQueued += queue.getPartition(i).size();
        }
        System.out.println("Unprocessed events remaining across all partitions: " + totalQueued + " (must be 0 after draining)");

        for (NotificationWorker worker : workers) {
            worker.stop();
        }
        for (Thread t : workerThreads) {
            t.join(500);
        }
        ((ExponentialBackoffRetryService) retryService).shutdown();
    }

    private static int orderingPartitionIndex(PartitionedNotificationQueue queue, String doorbellId) {
        // Mirrors PartitionedNotificationQueue's own hashing so the test recorder
        // listens on the SAME partition the real enqueue() calls will land on.
        return Math.floorMod(doorbellId.hashCode(), queue.getPartitionCount());
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
