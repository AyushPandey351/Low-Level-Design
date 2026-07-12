package meetingscheduler;

import java.util.List;

// The core entity. Notice reschedule() only flips `slot` - it does NOT re-check
// availability itself. That check ("is the NEW slot actually free for everyone")
// deliberately lives in MeetingScheduler/AvailabilityService instead, which is the
// concrete payoff of your notes' "availability logic should not be inside Meeting":
// Meeting stays a pure state holder, and the one place that knows how to check
// calendars is the one place you'd ever need to change if that logic evolves (e.g.
// adding room-capacity checks later touches AvailabilityService, never Meeting).
//
// Also notice Calendar never needs to be told about a reschedule: Calendar.meetings
// stores Meeting OBJECT REFERENCES, not copies of the slot. Once this Meeting's own
// `slot` field is mutated, every participant's calendar automatically reflects the
// new time the next time it's read - no separate "update the calendar" step required.
public class Meeting {
    private final String meetingId;
    private final String title;
    private final User organizer;
    private final List<Participant> participants;
    private TimeSlot slot;
    private MeetingStatus status;

    public Meeting(String meetingId, String title, User organizer, List<Participant> participants, TimeSlot slot) {
        this.meetingId = meetingId;
        this.title = title;
        this.organizer = organizer;
        this.participants = participants;
        this.slot = slot;
        schedule();
    }

    private void schedule() {
        status = MeetingStatus.SCHEDULED;
    }

    public void cancel() {
        requireStatus(MeetingStatus.SCHEDULED);
        status = MeetingStatus.CANCELLED;
    }

    public void reschedule(TimeSlot newSlot) {
        requireStatus(MeetingStatus.SCHEDULED);
        this.slot = newSlot;
    }

    public void addParticipant(User user) {
        participants.add(new Participant(user, InvitationStatus.PENDING));
    }

    public void removeParticipant(User user) {
        participants.removeIf(p -> p.getUser().equals(user));
    }

    public Participant getParticipant(User user) {
        for (Participant participant : participants) {
            if (participant.getUser().equals(user)) {
                return participant;
            }
        }
        return null;
    }

    private void requireStatus(MeetingStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Meeting " + meetingId + " must be " + expected + " but is " + status);
        }
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getTitle() {
        return title;
    }

    public User getOrganizer() {
        return organizer;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public MeetingStatus getStatus() {
        return status;
    }
}
