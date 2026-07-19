package ringiot;

public interface DeviceRepository {
    Device find(String deviceId);

    void save(Device device);

    void update(Device device);
}
