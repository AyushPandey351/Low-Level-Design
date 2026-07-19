package ring;

// Strategy Pattern, per your notes - NotificationService (via PushProviderFactory)
// depends on this INTERFACE, never a concrete FCMPushProvider directly. Adding a new
// provider (say, a smart-fridge display) tomorrow is a one-class addition, zero
// changes to NotificationService.
// send() takes Device as a second parameter, beyond the notes' literal
// `send(Notification)` signature - a provider needs the device's pushToken/platform
// to actually deliver anything, and Notification alone (eventId, deviceId, status)
// doesn't carry that. A small, necessary addition, same category as Device.homeId.
public interface PushProvider {
    void send(Notification notification, Device device);
}
