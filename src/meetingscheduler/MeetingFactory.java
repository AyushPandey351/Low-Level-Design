package meetingscheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Centralizes the id generation + User-to-Participant wrapping that would otherwise
// be repeated at every call site that creates a Meeting. The organizer is wrapped as
// ACCEPTED immediately (they're the one scheduling it - asking them to accept their
// own invite would be strange), while every invitee starts PENDING, awaiting a real
// response via Participant.accept()/decline().
public class MeetingFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public Meeting createMeeting(String title, User organizer, List<User> invitees, TimeSlot slot) {
        String meetingId = "MTG" + counter.incrementAndGet();

        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant(organizer, InvitationStatus.ACCEPTED));
        for (User invitee : invitees) {
            participants.add(new Participant(invitee, InvitationStatus.PENDING));
        }

        return new Meeting(meetingId, title, organizer, participants, slot);
    }
}
