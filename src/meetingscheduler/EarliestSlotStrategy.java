package meetingscheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// Scans candidate start times in fixed increments across the search window and
// returns the FIRST one that's free for every calendar - the simplest possible
// search algorithm, and the natural default. Step size (15 minutes) is a granularity
// choice, not a correctness one: smaller steps find a slightly earlier slot at the
// cost of more isAvailable() calls; it doesn't change the overlap logic itself,
// which TimeSlot.overlaps() and Calendar.isAvailable() already own.
public class EarliestSlotStrategy implements SchedulingStrategy {

    private static final Duration STEP = Duration.ofMinutes(15);

    @Override
    public TimeSlot findSlot(AvailabilityService availabilityService, List<Calendar> calendars,
                              Duration duration, TimeSlot searchWindow) {
        LocalDateTime candidateStart = searchWindow.getStart();
        LocalDateTime latestPossibleStart = searchWindow.getEnd().minus(duration);

        while (!candidateStart.isAfter(latestPossibleStart)) {
            TimeSlot candidate = new TimeSlot(candidateStart, candidateStart.plus(duration));
            if (availabilityService.isAvailable(calendars, candidate)) {
                return candidate;
            }
            candidateStart = candidateStart.plus(STEP);
        }
        throw new IllegalStateException("No common slot found in the given search window");
    }
}
