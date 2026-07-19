package ring;

import java.util.List;

// Repository Pattern from your notes - NotificationService depends on this
// INTERFACE, never a concrete database/cache implementation. The notes flag "this
// mapping is read-heavy, so cache it in Redis" - that caching decision lives inside
// whichever concrete implementation is wired in (see InMemoryDeviceRepository's
// comment), completely invisible to NotificationService.
public interface DeviceRepository {
    List<Device> findByHome(String homeId);

    void save(Device device);
}
