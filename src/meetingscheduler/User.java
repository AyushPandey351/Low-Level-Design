package meetingscheduler;

import java.util.List;

// Mostly a data-holder, same reasoning as User/Player/Customer in the earlier
// designs - createMeeting()/acceptInvite()/declineInvite() are left off because that
// logic needs MeetingScheduler (the coordinator), which User shouldn't reference.
//
// viewCalendar() IS kept here though, unlike those earlier designs' equivalent
// methods - and it's worth noticing WHY this one case is different: Calendar is a
// field User owns directly (composition, assigned once at construction), not
// something reached through a separate coordinator/repository. Delegating
// `return calendar.getMeetings()` creates no circular dependency and needs no
// back-reference to MeetingScheduler, so there's no structural reason to push it
// elsewhere. The test isn't "is this behavior mentioned in the notes" - it's
// "does implementing it here require reaching into a coordinator."
public class User {
    private final String userId;
    private final String name;
    private final String email;
    private final Calendar calendar;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.calendar = new Calendar("CAL_" + userId);
    }

    public List<Meeting> viewCalendar() {
        return calendar.getMeetings();
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

    public Calendar getCalendar() {
        return calendar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return userId.equals(((User) o).userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
