package notification;

// updatePreference() is kept here (unlike sendNotification-type behaviors on User in
// other designs in this series) because NotificationPreference is a field User owns
// directly by composition - replacing it needs no reference to NotificationService
// or any other coordinator, the same reasoning that let User.viewCalendar() stay on
// User in the Meeting Scheduler design.
public class User {
    private final String userId;
    private final String name;
    private final String email;
    private final String phone;
    private final String deviceToken;
    private NotificationPreference preference;

    public User(String userId, String name, String email, String phone, String deviceToken,
                NotificationPreference preference) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deviceToken = deviceToken;
        this.preference = preference;
    }

    public void updatePreference(NotificationPreference preference) {
        this.preference = preference;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public NotificationPreference getPreference() {
        return preference;
    }

    @Override
    public String toString() {
        return name;
    }
}
