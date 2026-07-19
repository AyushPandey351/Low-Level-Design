package ringiot;

import java.time.Instant;

// Simulates the mTLS certificate presented by a device - no real X.509/crypto here
// (same "ignore real crypto/providers" treatment as every external boundary in this
// series), just the THREE checks the notes call out: expiry, revocation, and
// signature match against what the server has on record for this device.
public class DeviceCertificate {
    private final String deviceId;
    private final String signature;
    private final Instant expiry;
    private final boolean revoked;

    public DeviceCertificate(String deviceId, String signature, Instant expiry, boolean revoked) {
        this.deviceId = deviceId;
        this.signature = signature;
        this.expiry = expiry;
        this.revoked = revoked;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSignature() {
        return signature;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
