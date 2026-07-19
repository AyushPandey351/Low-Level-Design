package ring;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

// Stands in for "Device table + Redis cache in front of it" from the notes -
// devices are indexed by homeId in a ConcurrentHashMap<String, List<Device>>, which
// is exactly the shape a Redis cache of this mapping would have (key: homeId,
// value: that home's device list) - O(1) lookup instead of a table scan, which is
// the whole point of caching a read-heavy mapping like this one.
public class InMemoryDeviceRepository implements DeviceRepository {
    private final Map<String, List<Device>> devicesByHome = new ConcurrentHashMap<>();

    @Override
    public List<Device> findByHome(String homeId) {
        return devicesByHome.getOrDefault(homeId, List.of()).stream()
                .filter(device -> device.getStatus() == DeviceStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Device device) {
        // CopyOnWriteArrayList, not ArrayList: computeIfAbsent only makes the LIST'S
        // creation atomic - appending to an already-existing plain ArrayList from
        // multiple concurrent save() calls (e.g. two devices in the same home
        // registering at once) would still race. Registrations are rare relative to
        // reads (findByHome runs on every doorbell press), the same read-heavy
        // profile that justified CopyOnWriteArrayList in EventPublisher earlier.
        devicesByHome.computeIfAbsent(device.getHomeId(), id -> new CopyOnWriteArrayList<>()).add(device);
    }
}
