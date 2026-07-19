package ring;

public class AlexaPushProvider implements PushProvider {
    @Override
    public void send(Notification notification, Device device) {
        System.out.println("[Alexa -> " + device.getPushToken() + "] \"Someone is at the door\" (event " + notification.getEventId() + ")");
    }
}
