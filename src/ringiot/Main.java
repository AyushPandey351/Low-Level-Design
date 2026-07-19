package ringiot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- Wiring ---
        DeviceRepository deviceRepository = new DynamoDbDeviceRepository();
        deviceRepository.save(new Device("cam001", "home9", "1.2.0"));
        deviceRepository.save(new Device("doorbell12", "home9", "2.0.1"));

        MutualTlsDeviceAuthenticator authenticator = new MutualTlsDeviceAuthenticator();
        authenticator.registerDevice("cam001", "sig-cam001");
        authenticator.registerDevice("doorbell12", "sig-doorbell12");

        DeviceRateLimiter rateLimiter = new DeviceRateLimiter(5, 2000);
        EventDeduplicationService dedup = new RedisEventDeduplicationService(Duration.ofSeconds(5));
        EventValidator validator = new CompositeEventValidator(
                List.of(new SchemaEventValidator(), new DeviceRegisteredValidator(deviceRepository)));
        SequenceGapDetector sequenceGapDetector = new SequenceGapDetector();
        DeadLetterQueue dlq = new DeadLetterQueue();

        KafkaTopic topic = new KafkaTopic(4);
        topic.registerConsumerGroup("notification");
        topic.registerConsumerGroup("storage");
        topic.registerConsumerGroup("analytics");

        DeviceIngestionService ingestionService = new DeviceIngestionService(
                authenticator, rateLimiter, dedup, validator, sequenceGapDetector,
                new KafkaEventPublisher(topic), dlq);

        NotificationService notificationService = new SimpleNotificationService();
        EventProcessorFactory processorFactory = new EventProcessorFactory();
        processorFactory.register(EventType.MOTION, new MotionProcessor(notificationService));
        processorFactory.register(EventType.BATTERY_LOW, new BatteryProcessor(deviceRepository, notificationService));
        processorFactory.register(EventType.CAMERA_OFFLINE, new OfflineProcessor(deviceRepository, notificationService));
        processorFactory.register(EventType.WIFI_DISCONNECTED, new OfflineProcessor(deviceRepository, notificationService));
        processorFactory.register(EventType.DOORBELL_PRESSED, new DoorbellProcessor(notificationService));
        processorFactory.register(EventType.DEVICE_REBOOTED, new RebootProcessor(deviceRepository));

        EventRepository eventRepository = new S3EventRepository();

        List<Thread> allThreads = new ArrayList<>();
        List<TopicConsumerWorker> allWorkers = new ArrayList<>();
        startConsumerGroup(topic, "notification", event -> processorFactory.getProcessor(event.getType()).process(event),
                allThreads, allWorkers);
        startConsumerGroup(topic, "storage", event -> {
            eventRepository.save(event);
            System.out.println("[Storage] persisted " + event.getEventId() + " to S3");
        }, allThreads, allWorkers);
        startConsumerGroup(topic, "analytics", event ->
                System.out.println("[Analytics] logged " + event.getEventId() + " for dashboards"), allThreads, allWorkers);

        // --- Basic flow: one valid MOTION event fans out to all 3 consumer groups ---
        System.out.println("== Valid MOTION event from cam001 ==");
        ingestionService.ingest(validEvent("e1", "cam001", EventType.MOTION, 1, Map.of()), validCert("cam001"));
        Thread.sleep(300);

        // --- Deduplication: same eventId ingested again ---
        System.out.println("\n== Same eventId (e1) ingested again (e.g. device retry after a network timeout) ==");
        ingestionService.ingest(validEvent("e1", "cam001", EventType.MOTION, 1, Map.of()), validCert("cam001"));
        Thread.sleep(200);

        // --- Authentication: revoked certificate ---
        System.out.println("\n== Event with a REVOKED certificate ==");
        DeviceCertificate revoked = new DeviceCertificate("cam001", "sig-cam001", Instant.now().plusSeconds(3600), true);
        ingestionService.ingest(validEvent("e2", "cam001", EventType.MOTION, 2, Map.of()), revoked);
        Thread.sleep(200);

        // --- DLQ: event references a device that was never registered in DeviceRepository,
        // but DOES authenticate fine (a valid, provisioned device whose registration record
        // is missing/not-yet-synced) - this is what actually reaches DeviceRegisteredValidator
        // rather than being rejected earlier at authentication. ---
        System.out.println("\n== Event from a device that authenticates but was never registered as a Device ==");
        authenticator.registerDevice("ghost001", "sig-ghost001");
        ingestionService.ingest(validEvent("e3", "ghost001", EventType.MOTION, 1, Map.of()), validCert("ghost001"));
        Thread.sleep(100);
        System.out.println("Dead letter queue: " + dlq.getAll());

        // --- Confirm the pipeline is unaffected: cam001 can still send normally ---
        System.out.println("\n== Confirming the pipeline still works after the DLQ event ==");
        ingestionService.ingest(validEvent("e4", "cam001", EventType.BATTERY_LOW, 3, Map.of("batteryLevel", 15)), validCert("cam001"));
        Thread.sleep(300);
        System.out.println("cam001 battery level now: " + deviceRepository.find("cam001").getBatteryLevel());

        // --- Sequence gap detection: sequence jumps from 3 to 7 ---
        System.out.println("\n== Sequence gap: cam001 jumps from sequence 3 straight to 7 ==");
        ingestionService.ingest(validEvent("e5", "cam001", EventType.MOTION, 7, Map.of()), validCert("cam001"));
        Thread.sleep(200);

        // --- Rate limiting + quarantine: doorbell12 sends too many events too fast ---
        System.out.println("\n== doorbell12 sends 6 events rapidly (limit is 5 per 2s window) ==");
        for (int i = 1; i <= 6; i++) {
            ingestionService.ingest(validEvent("db-evt" + i, "doorbell12", EventType.DOORBELL_PRESSED, i, Map.of()), validCert("doorbell12"));
        }
        Thread.sleep(200);
        System.out.println("\n== doorbell12 tries again while quarantined ==");
        ingestionService.ingest(validEvent("db-evt7", "doorbell12", EventType.DOORBELL_PRESSED, 7, Map.of()), validCert("doorbell12"));

        // --- Ordering proof, ISOLATED: a dedicated topic + recorder consumer group ---
        System.out.println("\n== Ordering: 3 events for the SAME deviceId, recorded by a dedicated consumer group ==");
        KafkaTopic orderingTopic = new KafkaTopic(4);
        orderingTopic.registerConsumerGroup("recorder");
        List<String> processingOrder = new CopyOnWriteArrayList<>();
        int partitionIndex = Math.floorMod("orderTestCam".hashCode(), orderingTopic.getPartitionCount());
        TopicConsumerWorker recorder = new TopicConsumerWorker(
                orderingTopic.getPartition("recorder", partitionIndex), event -> processingOrder.add(event.getEventId()));
        Thread recorderThread = new Thread(recorder, "order-recorder");
        recorderThread.start();
        orderingTopic.publish(validEvent("order-e1", "orderTestCam", EventType.MOTION, 1, Map.of()));
        orderingTopic.publish(validEvent("order-e2", "orderTestCam", EventType.MOTION, 2, Map.of()));
        orderingTopic.publish(validEvent("order-e3", "orderTestCam", EventType.MOTION, 3, Map.of()));
        Thread.sleep(300);
        recorder.stop();
        recorderThread.join();
        System.out.println("Processing order: " + processingOrder + " (must be exactly [order-e1, order-e2, order-e3])");

        // --- Real concurrency test: 50 devices, one event each, all at once ---
        System.out.println("\n== Concurrency: 50 devices send an event simultaneously ==");
        int deviceCount = 50;
        for (int i = 0; i < deviceCount; i++) {
            String id = "bulkCam" + i;
            deviceRepository.save(new Device(id, "bulkHome" + i, "1.0.0"));
            authenticator.registerDevice(id, "sig-" + id);
        }

        ExecutorService loadGenerator = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(deviceCount);
        for (int i = 0; i < deviceCount; i++) {
            String id = "bulkCam" + i;
            loadGenerator.submit(() -> {
                awaitLatch(startLatch);
                ingestionService.ingest(validEvent("bulk-" + id, id, EventType.MOTION, 1, Map.of()), validCert(id));
                doneLatch.countDown();
            });
        }
        startLatch.countDown();
        doneLatch.await();
        loadGenerator.shutdown();
        Thread.sleep(1000);

        System.out.println("Remaining in 'notification' group: " + topic.sizeOf("notification"));
        System.out.println("Remaining in 'storage' group: " + topic.sizeOf("storage"));
        System.out.println("Remaining in 'analytics' group: " + topic.sizeOf("analytics"));
        System.out.println("(all must be 0 - every consumer group independently drained its own full copy of the stream)");

        for (TopicConsumerWorker worker : allWorkers) {
            worker.stop();
        }
        for (Thread t : allThreads) {
            t.join(500);
        }
    }

    private static void startConsumerGroup(KafkaTopic topic, String groupName, EventHandler handler,
                                            List<Thread> allThreads, List<TopicConsumerWorker> allWorkers) {
        for (int i = 0; i < topic.getPartitionCount(); i++) {
            TopicConsumerWorker worker = new TopicConsumerWorker(topic.getPartition(groupName, i), handler);
            allWorkers.add(worker);
            Thread t = new Thread(worker, groupName + "-worker-" + i);
            t.start();
            allThreads.add(t);
        }
    }

    private static Event validEvent(String eventId, String deviceId, EventType type, long sequence, Map<String, Object> payload) {
        return new Event(eventId, deviceId, "home9", type, Instant.now(), sequence, payload);
    }

    private static DeviceCertificate validCert(String deviceId) {
        return new DeviceCertificate(deviceId, "sig-" + deviceId, Instant.now().plusSeconds(3600), false);
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
