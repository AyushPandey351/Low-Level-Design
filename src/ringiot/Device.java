package ringiot;

import java.time.Instant;

// Mutable, unlike Event - a Device represents ONGOING state (current battery,
// online/offline, last-seen time) that legitimately changes as new events arrive,
// same category as ShowSeat or Cell earlier in this series. Mutators are
// `synchronized` because multiple EventProcessor implementations, running on
// different consumer-group worker threads, can update the SAME device concurrently
// (a BatteryProcessor and an OfflineProcessor could both touch the same device at
// nearly the same instant).
public class Device {
    private final String deviceId;
    private final String homeId;
    private DeviceStatus status;
    private int batteryLevel;
    private final String firmwareVersion;
    private Instant lastSeen;

    public Device(String deviceId, String homeId, String firmwareVersion) {
        this.deviceId = deviceId;
        this.homeId = homeId;
        this.firmwareVersion = firmwareVersion;
        this.status = DeviceStatus.ONLINE;
        this.batteryLevel = 100;
        this.lastSeen = Instant.now();
    }

    public synchronized void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public synchronized void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public synchronized void touch() {
        this.lastSeen = Instant.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHomeId() {
        return homeId;
    }

    public synchronized DeviceStatus getStatus() {
        return status;
    }

    public synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public synchronized Instant getLastSeen() {
        return lastSeen;
    }
}
