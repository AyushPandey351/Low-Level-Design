package ring;

import java.util.EnumMap;
import java.util.Map;

// Factory Pattern from your notes - centralizes "given a Platform, give me the
// right PushProvider" instead of NotificationService branching on platform itself.
// Backed by a Map (with registerProvider() as an override hook) rather than a bare
// switch, mirroring NotificationFactory from the earlier Notification System design -
// same reasoning: it gives tests a clean way to substitute a FlakyPushProvider
// without touching the real provider classes.
public class PushProviderFactory {
    private final Map<Platform, PushProvider> providers = new EnumMap<>(Platform.class);

    public PushProviderFactory() {
        providers.put(Platform.IOS, new APNSPushProvider());
        providers.put(Platform.ANDROID, new FCMPushProvider());
        providers.put(Platform.ALEXA, new AlexaPushProvider());
        providers.put(Platform.WEB, new WebPushProvider());
    }

    public PushProvider getProvider(Platform platform) {
        PushProvider provider = providers.get(platform);
        if (provider == null) {
            throw new IllegalArgumentException("No push provider registered for platform: " + platform);
        }
        return provider;
    }

    public void registerProvider(Platform platform, PushProvider provider) {
        providers.put(platform, provider);
    }
}
