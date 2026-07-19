package ring;

public class WebPushProvider implements PushProvider {
    @Override
    public void send(Notification notification, Device device) {
        System.out.println("[WebPush -> " + device.getPushToken() + "] Doorbell ring (event " + notification.getEventId() + ")");
    }
}
