package meetingscheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// The orchestrator. Meetings themselves are looked up via a ConcurrentHashMap held
// directly here rather than a separate unlisted "MeetingRepository" class - your
// notes only call out CalendarRepository explicitly, and introducing a whole extra
// class purely to mirror that pattern for meetings would be scope beyond what's
// actually asked for.
//
// CalendarRepository IS used for real here (not just built because the notes
// mention it): calendars are looked up by userId through the repository rather than
// via user.getCalendar() directly, modeling the more realistic scenario where a
// Calendar's canonical location is a shared store, not a live object graph hanging
// off in-memory User instances.
//
// THE key correctness detail in this class is how locks are acquired in
// scheduleMeeting/cancelMeeting/rescheduleMeeting: every call sorts the calendars
// being locked by calendarId FIRST, then locks them in that fixed order. This is not
// just "Lock participant calendars" from your notes taken literally - if two
// concurrent requests both need to lock, say, {Alice, Charlie}, but one locks them in
// the order [Alice, Charlie] while the other (because the caller passed participants
// in a different order) locks [Charlie, Alice], you get a classic deadlock: request A
// holds Alice's lock waiting for Charlie's, request B holds Charlie's lock waiting
// for Alice's, and neither ever proceeds. Sorting by a stable key before locking
// guarantees EVERY concurrent request acquires locks in the same global order, which
// makes that circular-wait deadlock structurally impossible.
public class MeetingScheduler {
    private final AvailabilityService availabilityService;
    private final MeetingFactory meetingFactory;
    private final CalendarRepository calendarRepository;
    private final NotificationService notificationService;
    private final Map<String, Meeting> meetings = new ConcurrentHashMap<>();

    public MeetingScheduler(AvailabilityService availabilityService, MeetingFactory meetingFactory,
                             CalendarRepository calendarRepository, NotificationService notificationService) {
        this.availabilityService = availabilityService;
        this.meetingFactory = meetingFactory;
        this.calendarRepository = calendarRepository;
        this.notificationService = notificationService;
    }

    public void registerUser(User user) {
        calendarRepository.save(user.getUserId(), user.getCalendar());
    }

    public Meeting scheduleMeeting(String title, User organizer, List<User> invitees, TimeSlot requestedSlot) {
        List<User> allUsers = new ArrayList<>();
        allUsers.add(organizer);
        allUsers.addAll(invitees);
        List<Calendar> calendars = calendarsFor(allUsers);

        List<Calendar> lockOrder = sortForLocking(calendars);
        lockAll(lockOrder);
        try {
            if (!availabilityService.isAvailable(calendars, requestedSlot)) {
                throw new IllegalStateException("Requested slot conflicts with an existing meeting for one or more participants");
            }
            Meeting meeting = meetingFactory.createMeeting(title, organizer, invitees, requestedSlot);
            for (Calendar calendar : calendars) {
                calendar.addMeeting(meeting);
            }
            meetings.put(meeting.getMeetingId(), meeting);
            notificationService.sendInvitation(meeting);
            return meeting;
        } finally {
            unlockAll(lockOrder);
        }
    }

    public TimeSlot findCommonSlot(User organizer, List<User> invitees, Duration duration,
                                    TimeSlot searchWindow, SchedulingStrategy strategy) {
        List<User> allUsers = new ArrayList<>();
        allUsers.add(organizer);
        allUsers.addAll(invitees);
        return availabilityService.findCommonSlot(calendarsFor(allUsers), duration, searchWindow, strategy);
    }

    public void cancelMeeting(String meetingId) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        List<Calendar> calendars = calendarsForParticipants(meeting);

        List<Calendar> lockOrder = sortForLocking(calendars);
        lockAll(lockOrder);
        try {
            meeting.cancel();
            for (Calendar calendar : calendars) {
                calendar.removeMeeting(meeting);
            }
            notificationService.sendCancellation(meeting);
        } finally {
            unlockAll(lockOrder);
        }
    }

    public void rescheduleMeeting(String meetingId, TimeSlot newSlot) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        List<Calendar> calendars = calendarsForParticipants(meeting);

        List<Calendar> lockOrder = sortForLocking(calendars);
        lockAll(lockOrder);
        try {
            if (!availabilityService.isAvailable(calendars, newSlot, meetingId)) {
                throw new IllegalStateException("New slot conflicts with an existing meeting for one or more participants");
            }
            meeting.reschedule(newSlot);
            notificationService.sendInvitation(meeting);
        } finally {
            unlockAll(lockOrder);
        }
    }

    public void acceptInvite(String meetingId, String userId) {
        respondToInvite(meetingId, userId, Participant::accept);
    }

    public void declineInvite(String meetingId, String userId) {
        respondToInvite(meetingId, userId, Participant::decline);
    }

    private void respondToInvite(String meetingId, String userId, java.util.function.Consumer<Participant> response) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        for (Participant participant : meeting.getParticipants()) {
            if (participant.getUser().getUserId().equals(userId)) {
                response.accept(participant);
                return;
            }
        }
        throw new IllegalArgumentException("User " + userId + " is not a participant of meeting " + meetingId);
    }

    private List<Calendar> calendarsFor(List<User> users) {
        return users.stream()
                .map(user -> calendarRepository.find(user.getUserId()))
                .collect(Collectors.toList());
    }

    private List<Calendar> calendarsForParticipants(Meeting meeting) {
        return meeting.getParticipants().stream()
                .map(participant -> calendarRepository.find(participant.getUser().getUserId()))
                .collect(Collectors.toList());
    }

    private List<Calendar> sortForLocking(List<Calendar> calendars) {
        return calendars.stream()
                .sorted(Comparator.comparing(Calendar::getCalendarId))
                .collect(Collectors.toList());
    }

    private void lockAll(List<Calendar> calendars) {
        for (Calendar calendar : calendars) {
            calendar.getLock().lock();
        }
    }

    private void unlockAll(List<Calendar> calendars) {
        for (int i = calendars.size() - 1; i >= 0; i--) {
            calendars.get(i).getLock().unlock();
        }
    }

    private Meeting getMeetingOrThrow(String meetingId) {
        Meeting meeting = meetings.get(meetingId);
        if (meeting == null) {
            throw new IllegalArgumentException("No such meeting: " + meetingId);
        }
        return meeting;
    }
}
