package meetingscheduler;

import java.time.Duration;
import java.util.List;

// WRAPS a fallback SchedulingStrategy rather than reimplementing a search - same
// composition technique as WeekendPricingStrategy (Parking Lot) and
// PreferredSlotStrategy's namesake in your notes: try the organizer's preferred slot
// first, and only if that's actually taken, fall back to whatever search algorithm
// the fallback strategy implements (typically EarliestSlotStrategy). This means
// "preferred, with earliest-available fallback" doesn't need its own scanning logic
// at all - it reuses whatever fallback is plugged in, staying correct even if the
// fallback strategy's own algorithm changes later.
public class PreferredSlotStrategy implements SchedulingStrategy {

    private final TimeSlot preferredSlot;
    private final SchedulingStrategy fallback;

    public PreferredSlotStrategy(TimeSlot preferredSlot, SchedulingStrategy fallback) {
        this.preferredSlot = preferredSlot;
        this.fallback = fallback;
    }

    @Override
    public TimeSlot findSlot(AvailabilityService availabilityService, List<Calendar> calendars,
                              Duration duration, TimeSlot searchWindow) {
        if (availabilityService.isAvailable(calendars, preferredSlot)) {
            return preferredSlot;
        }
        return fallback.findSlot(availabilityService, calendars, duration, searchWindow);
    }
}
