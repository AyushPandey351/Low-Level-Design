package ringiot;

// The core orchestrator, matching the notes' ingestion flow: authenticate first
// ("never trust a device because it knows a deviceId"), then a strict pipeline of
// cheap-to-expensive checks, each of which can short-circuit the rest -
// deliberately ordered so the CHEAPEST rejections happen FIRST:
//   1. Authentication - a forged/expired/revoked device is rejected before it
//      consumes ANY other resource.
//   2. Quarantine check - free (a Set lookup) - reject a known-bad device instantly.
//   3. Rate limit - cheap - catches a misbehaving device BEFORE wasting effort on
//      dedup/validation for its flood of events.
//   4. Deduplication - a hash map lookup - cheaper than full schema validation.
//   5. Validation - the most expensive check, run last among the rejects, since by
//      this point we've already filtered out untrusted, quarantined, rate-limited,
//      and duplicate events; only genuinely NEW, legitimate-looking events reach it.
//   6. Sequence gap detection - never blocks, just logs an anomaly.
//   7. Publish - the only thing that touches Kafka, and only for events that
//      survived every check above. This method returns as soon as publish()
//      returns - "should return quickly, never perform notification logic
//      synchronously" - all the actual notification/storage/analytics work happens
//      later, on the consumer-group worker threads.
public class DeviceIngestionService {
    private final DeviceAuthenticator authenticator;
    private final DeviceRateLimiter rateLimiter;
    private final EventDeduplicationService deduplicationService;
    private final EventValidator validator;
    private final SequenceGapDetector sequenceGapDetector;
    private final EventPublisher publisher;
    private final DeadLetterQueue deadLetterQueue;

    public DeviceIngestionService(DeviceAuthenticator authenticator, DeviceRateLimiter rateLimiter,
                                   EventDeduplicationService deduplicationService, EventValidator validator,
                                   SequenceGapDetector sequenceGapDetector, EventPublisher publisher,
                                   DeadLetterQueue deadLetterQueue) {
        this.authenticator = authenticator;
        this.rateLimiter = rateLimiter;
        this.deduplicationService = deduplicationService;
        this.validator = validator;
        this.sequenceGapDetector = sequenceGapDetector;
        this.publisher = publisher;
        this.deadLetterQueue = deadLetterQueue;
    }

    public void ingest(Event event, DeviceCertificate certificate) {
        if (!authenticator.authenticate(certificate)) {
            System.out.println("[Ingestion] REJECTED " + event.getEventId() + " - authentication failed for device " + event.getDeviceId());
            return;
        }

        if (rateLimiter.isQuarantined(event.getDeviceId())) {
            System.out.println("[Ingestion] REJECTED " + event.getEventId() + " - device " + event.getDeviceId() + " is quarantined");
            return;
        }

        if (!rateLimiter.allow(event.getDeviceId())) {
            return; // DeviceRateLimiter already logged the quarantine action
        }

        if (!deduplicationService.trySetIfAbsent(event.getEventId())) {
            System.out.println("[Ingestion] Duplicate event " + event.getEventId() + " ignored");
            return;
        }

        try {
            validator.validate(event);
        } catch (InvalidEventException e) {
            deadLetterQueue.add(event, e.getMessage());
            System.out.println("[Ingestion] " + event.getEventId() + " routed to DLQ: " + e.getMessage());
            return;
        }

        sequenceGapDetector.check(event);
        publisher.publish(event);
    }
}
