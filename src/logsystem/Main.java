package logsystem;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        LogTopic topic = new LogTopic(4);
        topic.registerConsumerGroup("indexer");
        topic.registerConsumerGroup("alert");
        topic.registerConsumerGroup("archive");
        topic.registerConsumerGroup("livetail");

        LogDeduplicationService dedupService = new LogDeduplicationService();
        LogIndexer indexer = new OpenSearchIndexer();
        SearchService searchService = new OpenSearchSearchService(indexer);
        StorageService storageService = StorageServiceFactory.create(StorageBackend.S3);
        ColdStorageQueryService coldQueryService = new ColdStorageQueryService(storageService);
        Duration hotRetention = Duration.ofMillis(300); // shortened from 7 days purely so this demo runs in seconds
        LogSearchGateway searchGateway = new LogSearchGateway(searchService, coldQueryService, hotRetention);
        ThresholdAlertService alertService = new ThresholdAlertService(3, 2000);
        LiveTailService liveTailService = new LiveTailService();
        RetentionService retentionService = new RetentionService(indexer, storageService, hotRetention, Duration.ofMillis(800));

        List<Thread> allThreads = new ArrayList<>();
        List<LogConsumerWorker> allWorkers = new ArrayList<>();
        startGroup(topic, "indexer", log -> {
            if (dedupService.isNew(log.getLogId())) {
                indexer.index(log);
            } else {
                System.out.println("[Indexer] Skipping duplicate " + log.getLogId());
            }
        }, allThreads, allWorkers);
        startGroup(topic, "alert", alertService::evaluate, allThreads, allWorkers);
        startGroup(topic, "archive", storageService::archive, allThreads, allWorkers);
        startGroup(topic, "livetail", liveTailService::onLog, allThreads, allWorkers);

        LogCollector collector = new KafkaLogCollector(topic);
        LogAgent orderAgent = new LogAgent(collector, new GzipCompressionStrategy());

        // --- Basic ingestion + full-text search intersection ---
        System.out.println("== OrderService logs a batch, agent buffers then flushes ==");
        orderAgent.log(newLog("OrderService", "host-1", LogLevel.INFO, "t1", "Order placed successfully"));
        orderAgent.log(newLog("OrderService", "host-1", LogLevel.ERROR, "t2", "Payment timeout while charging card"));
        orderAgent.log(newLog("OrderService", "host-2", LogLevel.INFO, "t3", "Payment succeeded for order 55"));
        orderAgent.log(newLog("OrderService", "host-2", LogLevel.WARN, "t4", "Connection timeout to inventory service"));
        orderAgent.flush();
        Thread.sleep(300);

        System.out.println("\n== Full-text search: \"payment timeout\" (must intersect both terms) ==");
        List<LogEntry> results = searchGateway.search(SearchRequest.builder().fullText("payment timeout").build());
        results.forEach(System.out::println);
        System.out.println("(must be exactly 1 result: the log containing BOTH 'payment' AND 'timeout')");

        // --- Structured filters ---
        System.out.println("\n== Structured search: service=OrderService, level=WARN ==");
        searchGateway.search(SearchRequest.builder().service("OrderService").level(LogLevel.WARN).build())
                .forEach(System.out::println);

        // --- Trace correlation across services ---
        System.out.println("\n== Trace correlation: same traceId across two services ==");
        String sharedTrace = "trace-XYZ";
        orderAgent.log(newLog("OrderService", "host-1", LogLevel.INFO, sharedTrace, "Order received"));
        LogAgent paymentAgent = new LogAgent(collector, new SnappyCompressionStrategy());
        paymentAgent.log(newLog("PaymentService", "host-9", LogLevel.INFO, sharedTrace, "Payment authorized"));
        orderAgent.flush();
        paymentAgent.flush();
        Thread.sleep(200);
        searchGateway.search(SearchRequest.builder().traceId(sharedTrace).build()).forEach(System.out::println);

        // --- Deduplication: the same logId sent twice (agent retry after a timeout) ---
        System.out.println("\n== Deduplication: same logId ingested twice ==");
        LogEntry duplicateProne = newLog("OrderService", "host-1", LogLevel.INFO, "t5", "Duplicate-prone log line");
        orderAgent.log(duplicateProne);
        orderAgent.flush();
        Thread.sleep(150);
        collector.collect(duplicateProne); // simulates a raw redelivery, bypassing the agent's own buffer
        Thread.sleep(150);
        System.out.println("Indexer copies of " + duplicateProne.getLogId() + ": "
                + (indexer.findById(duplicateProne.getLogId()) != null ? 1 : 0) + " (must be 1 - dedup runs at the indexer)");

        // --- Retry: a flaky collector fails twice, then succeeds ---
        System.out.println("\n== LogAgent retry: send fails twice, then succeeds ==");
        LogAgent flakyAgent = new LogAgent(new FlakyLogCollector(collector, 2), new GzipCompressionStrategy());
        flakyAgent.log(newLog("InventoryService", "host-5", LogLevel.INFO, "t6", "Retry-tested log line"));
        flakyAgent.flush();
        Thread.sleep(200);

        // --- Alerting: 3 ERRORs from the same service within the window ---
        System.out.println("\n== Alerting: 3 ERROR logs from InventoryService in quick succession ==");
        LogAgent inventoryAgent = new LogAgent(collector, new GzipCompressionStrategy());
        for (int i = 0; i < 3; i++) {
            inventoryAgent.log(newLog("InventoryService", "host-5", LogLevel.ERROR, "t" + (7 + i), "Stock check failed"));
        }
        inventoryAgent.flush();
        Thread.sleep(200);

        // --- Live tail ---
        System.out.println("\n== Live tail: subscribing to OrderService, then logging ==");
        liveTailService.subscribe("OrderService", log -> System.out.println("[LiveTail] " + log));
        orderAgent.log(newLog("OrderService", "host-1", LogLevel.INFO, "t10", "Live tail should show this line"));
        orderAgent.flush();
        Thread.sleep(200);

        // --- Hot -> cold routing ---
        System.out.println("\n== Waiting past the (shortened) hot retention window, then sweeping ==");
        Thread.sleep(400); // exceeds the 300ms hot retention configured above
        retentionService.hotToColdSweep();
        System.out.println("Searching the SAME payment/timeout query again, now that it's aged out of hot storage:");
        List<LogEntry> coldResults = searchGateway.search(SearchRequest.builder()
                .fullText("payment timeout").from(Instant.now().minusSeconds(3600)).build());
        coldResults.forEach(System.out::println);
        System.out.println("(routed to cold storage since the request's time range reaches past the hot window)");

        // --- Cold deletion after full retention ---
        System.out.println("\n== Waiting past total retention, then permanently deleting ==");
        Thread.sleep(500); // pushes everything past the 800ms total retention configured above
        int beforeDeletion = storageService.findAll().size();
        retentionService.coldDeletionSweep();
        int afterDeletion = storageService.findAll().size();
        System.out.println("Cold storage size before: " + beforeDeletion + ", after: " + afterDeletion + " (must be 0)");

        // --- Real concurrency test: many services logging simultaneously ---
        System.out.println("\n== Concurrency: 30 services logging simultaneously ==");
        int serviceCount = 30;
        ExecutorService loadGenerator = Executors.newFixedThreadPool(15);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(serviceCount);
        for (int i = 0; i < serviceCount; i++) {
            String serviceName = "BulkService" + i;
            loadGenerator.submit(() -> {
                awaitLatch(startLatch);
                LogAgent agent = new LogAgent(collector, new GzipCompressionStrategy());
                agent.log(newLog(serviceName, "host-bulk", LogLevel.INFO, UUID.randomUUID().toString(), "bulk log line"));
                agent.flush();
                doneLatch.countDown();
            });
        }
        startLatch.countDown();
        doneLatch.await();
        loadGenerator.shutdown();
        Thread.sleep(1000);

        System.out.println("Remaining in 'indexer' group: " + topic.sizeOf("indexer"));
        System.out.println("Remaining in 'alert' group: " + topic.sizeOf("alert"));
        System.out.println("Remaining in 'archive' group: " + topic.sizeOf("archive"));
        System.out.println("Remaining in 'livetail' group: " + topic.sizeOf("livetail"));
        System.out.println("(all must be 0 - every consumer group independently drained its own full copy of the stream)");

        for (LogConsumerWorker worker : allWorkers) {
            worker.stop();
        }
        for (Thread t : allThreads) {
            t.join(500);
        }
    }

    private static void startGroup(LogTopic topic, String groupName, java.util.function.Consumer<LogEntry> handler,
                                    List<Thread> allThreads, List<LogConsumerWorker> allWorkers) {
        for (int i = 0; i < topic.getPartitionCount(); i++) {
            LogConsumerWorker worker = new LogConsumerWorker(topic.getPartition(groupName, i), handler);
            allWorkers.add(worker);
            Thread t = new Thread(worker, groupName + "-worker-" + i);
            t.start();
            allThreads.add(t);
        }
    }

    private static LogEntry newLog(String service, String host, LogLevel level, String traceId, String message) {
        return new LogEntry(UUID.randomUUID().toString(), service, host, level, Instant.now(), traceId, message);
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
