package meetingscheduler;

import java.time.Duration;
import java.util.List;

// Strategy Pattern, same shape as every prior *Strategy interface in this series -
// MeetingScheduler holds this INTERFACE (via AvailabilityService.findCommonSlot),
// never a concrete EarliestSlotStrategy/PreferredSlotStrategy directly. That's the
// DIP callout in your notes, and it's what makes "nearest slot," "working-hours-only,"
// or "AI-based scheduling" a one-class addition later with zero changes to
// MeetingScheduler or AvailabilityService.
//
// Takes the AvailabilityService itself as a parameter rather than duplicating
// calendar-checking logic inside each strategy - a strategy's job is to decide WHICH
// candidate slots to try and in WHAT ORDER; checking whether any given candidate is
// actually free is AvailabilityService's job, reused as a primitive here.
public interface SchedulingStrategy {
    TimeSlot findSlot(AvailabilityService availabilityService, List<Calendar> calendars,
                       Duration duration, TimeSlot searchWindow);
}
