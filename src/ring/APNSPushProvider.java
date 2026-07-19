package ring;

public class APNSPushProvider implements PushProvider {
    @Override
    public void send(Notification notification, Device device) {
        System.out.println("[APNS -> " + device.getPushToken() + "] Doorbell ring (event " + notification.getEventId() + ")");
    }
}
