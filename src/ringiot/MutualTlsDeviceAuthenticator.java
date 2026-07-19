package ringiot;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutualTlsDeviceAuthenticator implements DeviceAuthenticator {
    private final Map<String, String> knownSignatures = new ConcurrentHashMap<>();

    public void registerDevice(String deviceId, String signature) {
        knownSignatures.put(deviceId, signature);
    }

    @Override
    public boolean authenticate(DeviceCertificate certificate) {
        if (certificate.isRevoked()) {
            return false;
        }
        if (certificate.getExpiry().isBefore(Instant.now())) {
            return false;
        }
        String expectedSignature = knownSignatures.get(certificate.getDeviceId());
        return expectedSignature != null && expectedSignature.equals(certificate.getSignature());
    }
}
