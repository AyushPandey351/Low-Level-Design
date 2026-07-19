package ringiot;

// "Never trust a device because it knows a deviceId" - this interface is the whole
// point made structural: DeviceIngestionService calls this BEFORE anything else
// (before validation, before dedup, before touching Kafka at all). A device that
// fails authentication never gets far enough to consume any other resource in the
// pipeline.
public interface DeviceAuthenticator {
    boolean authenticate(DeviceCertificate certificate);
}
