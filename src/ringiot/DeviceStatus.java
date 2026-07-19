package ringiot;

// QUARANTINED is deliberately NOT a value here - quarantine state (see RateLimiter)
// is owned solely by the rate limiter, not duplicated onto Device, so there's only
// ever one source of truth for "is this device currently quarantined."
public enum DeviceStatus {
    ONLINE,
    OFFLINE
}
