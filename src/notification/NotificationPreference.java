package notification;

import java.util.List;

// Stores which channels a user actually wants, and which CATEGORIES of notification
// they're willing to receive at all. NotificationService consults this BEFORE
// fanning a send request out across channels - "Allow users to configure channel
// preferences" is a real functional requirement, not just a data field to store and
// ignore.
public class NotificationPreference {
    private final List<ChannelType> preferredChannels;
    private final boolean promotionalEnabled;
    private final boolean transactionalEnabled;

    public NotificationPreference(List<ChannelType> preferredChannels, boolean promotionalEnabled,
                                   boolean transactionalEnabled) {
        this.preferredChannels = preferredChannels;
        this.promotionalEnabled = promotionalEnabled;
        this.transactionalEnabled = transactionalEnabled;
    }

    public boolean allowsChannel(ChannelType channel) {
        return preferredChannels.contains(channel);
    }

    public List<ChannelType> getPreferredChannels() {
        return preferredChannels;
    }

    public boolean isPromotionalEnabled() {
        return promotionalEnabled;
    }

    public boolean isTransactionalEnabled() {
        return transactionalEnabled;
    }
}
