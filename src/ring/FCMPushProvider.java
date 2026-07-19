package ring;

// Simulated - per the assumption of ignoring real APNS/FCM integration, this stands
// in for an actual Firebase Cloud Messaging call, same simulated-boundary approach
// used for every external provider throughout this series.
public class FCMPushProvider implements PushProvider {
    @Override
    public void send(Notification notification, Device device) {
        System.out.println("[FCM -> " + device.getPushToken() + "] Doorbell ring (event " + notification.getEventId() + ")");
    }
}
