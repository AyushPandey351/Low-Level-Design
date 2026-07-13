package ratelimiter;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // --- Fixed Window: 5 requests per 1-second window ---
        System.out.println("== Fixed Window: 5 requests / 1 second ==");
        RateLimiter limiter = RateLimiter.initialize(RateLimiterConfig.builder()
                .maxRequests(5).window(Duration.ofSeconds(1)).algorithm(AlgorithmType.FIXED_WINDOW).build());
        for (int i = 1; i <= 7; i++) {
            System.out.println("Request " + i + ": " + (limiter.allowRequest("user1") ? "ALLOWED" : "REJECTED (429)"));
        }
        System.out.println("Resetting user1 mid-window...");
        limiter.reset("user1");
        System.out.println("Request after reset: " + (limiter.allowRequest("user1") ? "ALLOWED" : "REJECTED (429)"));

        System.out.println("Waiting for the window to roll over...");
        Thread.sleep(1100);
        System.out.println("Request after window rollover: " + (limiter.allowRequest("user1") ? "ALLOWED" : "REJECTED (429)"));

        // --- Sliding Window: fixes the window-boundary burst problem ---
        System.out.println("\n== Sliding Window: 3 requests / 1 second ==");
        limiter.updateConfig(RateLimiterConfig.builder()
                .maxRequests(3).window(Duration.ofSeconds(1)).algorithm(AlgorithmType.SLIDING_WINDOW).build());
        for (int i = 1; i <= 4; i++) {
            System.out.println("Request " + i + ": " + (limiter.allowRequest("user2") ? "ALLOWED" : "REJECTED (429)"));
        }
        System.out.println("Waiting 1.1s for all 3 timestamps to slide out of the window...");
        Thread.sleep(1100);
        System.out.println("Request after sliding out: " + (limiter.allowRequest("user2") ? "ALLOWED" : "REJECTED (429)"));

        // --- Token Bucket: burst allowance, then throttled by refill rate ---
        System.out.println("\n== Token Bucket: capacity 5, refills to 5 tokens per second ==");
        limiter.updateConfig(RateLimiterConfig.builder()
                .maxRequests(5).window(Duration.ofSeconds(1)).algorithm(AlgorithmType.TOKEN_BUCKET).build());
        for (int i = 1; i <= 6; i++) {
            System.out.println("Request " + i + ": " + (limiter.allowRequest("user3") ? "ALLOWED" : "REJECTED (429)"));
        }
        System.out.println("Waiting 300ms for partial refill (~1.5 tokens)...");
        Thread.sleep(300);
        System.out.println("Request after partial refill: " + (limiter.allowRequest("user3") ? "ALLOWED" : "REJECTED (429)"));

        // --- Leaky Bucket: starts EMPTY, no initial burst allowance beyond capacity ---
        System.out.println("\n== Leaky Bucket: capacity 5, leaks at 5 per second ==");
        limiter.updateConfig(RateLimiterConfig.builder()
                .maxRequests(5).window(Duration.ofSeconds(1)).algorithm(AlgorithmType.LEAKY_BUCKET).build());
        for (int i = 1; i <= 6; i++) {
            System.out.println("Request " + i + ": " + (limiter.allowRequest("user4") ? "ALLOWED" : "REJECTED (429)"));
        }
        System.out.println("Waiting 300ms for partial leak (~1.5 units)...");
        Thread.sleep(300);
        System.out.println("Request after partial leak: " + (limiter.allowRequest("user4") ? "ALLOWED" : "REJECTED (429)"));

        // --- Remove API ---
        System.out.println("\n== remove(clientId) clears all tracked state for that client ==");
        limiter.remove("user4");
        System.out.println("Request right after remove (bucket rebuilt fresh): "
                + (limiter.allowRequest("user4") ? "ALLOWED" : "REJECTED (429)"));

        // --- Real concurrency test: 200 threads hammer the SAME client at once, limit=50 ---
        System.out.println("\n== Concurrency: 200 threads, limit=50 per window, same clientId, all at once ==");
        limiter.updateConfig(RateLimiterConfig.builder()
                .maxRequests(50).window(Duration.ofSeconds(10)).algorithm(AlgorithmType.FIXED_WINDOW).build());

        int threadCount = 200;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                awaitLatch(startLatch);
                if (limiter.allowRequest("racer")) {
                    allowedCount.incrementAndGet();
                }
                doneLatch.countDown();
            });
        }
        startLatch.countDown(); // release all 200 threads at once
        doneLatch.await();
        pool.shutdown();

        System.out.println("Allowed: " + allowedCount.get() + " out of " + threadCount + " concurrent requests"
                + " (must be exactly 50 - never more, proving the synchronized WindowCounter serializes correctly)");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
