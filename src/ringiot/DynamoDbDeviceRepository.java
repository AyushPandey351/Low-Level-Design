package ringiot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Stands in for "hot data - current device state - stored in DynamoDB" from the
// notes: keyed by deviceId, O(1) point lookups - exactly DynamoDB's access pattern
// for a table keyed on a single partition key, just without the actual AWS SDK call.
public class DynamoDbDeviceRepository implements DeviceRepository {
    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    @Override
    public Device find(String deviceId) {
        return devices.get(deviceId);
    }

    @Override
    public void save(Device device) {
        devices.put(device.getDeviceId(), device);
    }

    @Override
    public void update(Device device) {
        devices.put(device.getDeviceId(), device);
    }
}
