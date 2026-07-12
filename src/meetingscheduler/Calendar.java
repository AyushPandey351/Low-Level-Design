package meetingscheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// Owns one user's meetings AND the lock used to serialize concurrent scheduling
// attempts against them (see Step 8: "Calendar Lock"). The lock lives HERE, not in
// MeetingScheduler, because a Calendar is the resource being contended over - the
// same reasoning as a database row owning its own row-lock rather than the
// application server tracking locks separately.
//
// isAvailable() takes an optional `excludingMeetingId` for one specific reason:
// rescheduling a meeting must not treat that meeting's OWN current booking as a
// conflict with itself. Without the exclusion, calling isAvailable() while
// rescheduling would always fail (the meeting always overlaps its own existing slot).
public class Calendar {
    private final String calendarId;
    private final List<Meeting> meetings = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Calendar(String calendarId) {
        this.calendarId = calendarId;
    }

    public void addMeeting(Meeting meeting) {
        meetings.add(meeting);
    }

    public void removeMeeting(Meeting meeting) {
        meetings.remove(meeting);
    }

    public List<Meeting> getMeetings() {
        return meetings;
    }

    public boolean isAvailable(TimeSlot slot, String excludingMeetingId) {
        for (Meeting meeting : meetings) {
            if (meeting.getStatus() != MeetingStatus.SCHEDULED) {
                continue;
            }
            if (excludingMeetingId != null && meeting.getMeetingId().equals(excludingMeetingId)) {
                continue;
            }
            if (meeting.getSlot().overlaps(slot)) {
                return false;
            }
        }
        return true;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
