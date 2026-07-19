package ring;

// One addition beyond the notes' literal property list: `homeId`. DeviceRepository's
// whole contract is findByHome(homeId) - without Device carrying its own homeId,
// there'd be no way to actually answer "which devices belong to this home" from a
// flat device store. Same kind of small, necessary addition as Ticket.transactionId
// or Payment.paymentId earlier in this series - required for the class to actually
// fulfill the responsibility the notes already assign it.
public class Device {
    private final String deviceId;
    private final String userId;
    private final String homeId;
    private final String pushToken;
    private final Platform platform;
    private DeviceStatus status;

    public Device(String deviceId, String userId, String homeId, String pushToken, Platform platform) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.homeId = homeId;
        this.pushToken = pushToken;
        this.platform = platform;
        this.status = DeviceStatus.ACTIVE;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getHomeId() {
        return homeId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public Platform getPlatform() {
        return platform;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }
}
