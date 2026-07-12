package meetingscheduler;

import java.time.Duration;
import java.util.List;

// Interviewers love this separation for a reason: Meeting never touches Calendar
// directly to answer "is everyone free" - that logic lives here instead, as its own
// class with its own testable behavior. Two responsibilities, cleanly split:
//   - isAvailable(): the PRIMITIVE - "is this exact candidate slot free across all
//     these calendars" (used both to validate a directly-requested slot, and as a
//     building block that SchedulingStrategy implementations call repeatedly).
//   - findCommonSlot(): delegates the actual SEARCH ALGORITHM (which candidates to
//     try, in what order) to a SchedulingStrategy - this class doesn't know or care
//     whether that search tries "earliest first" or "preferred slot, then earliest."
public class AvailabilityService {

    public boolean isAvailable(List<Calendar> calendars, TimeSlot slot) {
        return isAvailable(calendars, slot, null);
    }

    public boolean isAvailable(List<Calendar> calendars, TimeSlot slot, String excludingMeetingId) {
        for (Calendar calendar : calendars) {
            if (!calendar.isAvailable(slot, excludingMeetingId)) {
                return false;
            }
        }
        return true;
    }

    public TimeSlot findCommonSlot(List<Calendar> calendars, Duration duration, TimeSlot searchWindow,
                                   SchedulingStrategy strategy) {
        return strategy.findSlot(this, calendars, duration, searchWindow);
    }
}
