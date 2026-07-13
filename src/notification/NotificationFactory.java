package notification;

import java.util.EnumMap;
import java.util.Map;

// Factory Pattern: centralizes "given a ChannelType, give me the right channel"
// instead of NotificationService (or anywhere else) branching on channel type
// itself. Backed by a Map rather than a switch-per-call, which also gives
// registerChannel() a natural home - useful both for genuinely swapping providers
// (e.g. a different email vendor) and for tests that need to substitute a
// FlakyChannelWrapper in place of the real channel.
public class NotificationFactory {
    private final Map<ChannelType, NotificationChannel> channels = new EnumMap<>(ChannelType.class);

    public NotificationFactory() {
        channels.put(ChannelType.EMAIL, new EmailNotificationChannel());
        channels.put(ChannelType.SMS, new SMSNotificationChannel());
        channels.put(ChannelType.PUSH, new PushNotificationChannel());
        channels.put(ChannelType.WHATSAPP, new WhatsAppNotificationChannel());
    }

    public NotificationChannel getChannel(ChannelType type) {
        NotificationChannel channel = channels.get(type);
        if (channel == null) {
            throw new IllegalArgumentException("Unknown channel type: " + type);
        }
        return channel;
    }

    public void registerChannel(ChannelType type, NotificationChannel channel) {
        channels.put(type, channel);
    }
}
