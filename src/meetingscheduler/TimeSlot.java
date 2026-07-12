package meetingscheduler;

import java.time.Duration;
import java.time.LocalDateTime;

// Your notes' star callout: encapsulating start/end into one type means overlaps()
// and duration() are defined ONCE, here, instead of every caller (Calendar,
// AvailabilityService, SchedulingStrategy) re-implementing the same interval-overlap
// math with raw LocalDateTime pairs - and getting the boundary condition subtly wrong
// in one of those places.
//
// overlaps() uses the classic interval-overlap test: two half-open intervals
// [s1, e1) and [s2, e2) overlap iff s1 < e2 AND s2 < e1. Note this treats a meeting
// ending at 10:00 and another starting at 10:00 as NOT overlapping (back-to-back
// meetings are fine) - getting this boundary right in exactly one place is the whole
// point of giving TimeSlot its own class rather than passing two LocalDateTimes around.
public class TimeSlot {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start (" + start + ") must be before end (" + end + ")");
        }
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return start + " - " + end;
    }
}
