package meetingscheduler;

// Observer Pattern from your notes: MeetingScheduler doesn't know or care HOW
// participants get notified (email/push/SMS) - it just calls this one class after a
// state change, decoupling "a meeting changed" from "what happens as a result."
//
// Per the assumptions ("ignore notifications"), there's no real email/SMS provider
// wired in here - same simulated-boundary approach as Merchant.receiveWebhook and
// the GatewayConnector implementations in the Payment Gateway design. The
// architectural seam (Observer) is still worth building even though the concrete
// delivery mechanism is out of scope - it's the seam a real NotificationService
// would plug into later.
public class NotificationService {

    public void sendInvitation(Meeting meeting) {
        for (Participant participant : meeting.getParticipants()) {
            System.out.println("[Notify] " + participant.getUser().getName()
                    + ": invited to '" + meeting.getTitle() + "' at " + meeting.getSlot());
        }
    }

    public void sendCancellation(Meeting meeting) {
        for (Participant participant : meeting.getParticipants()) {
            System.out.println("[Notify] " + participant.getUser().getName()
                    + ": '" + meeting.getTitle() + "' has been cancelled");
        }
    }

    public void sendReminder(Meeting meeting) {
        for (Participant participant : meeting.getParticipants()) {
            System.out.println("[Notify] " + participant.getUser().getName()
                    + ": reminder - '" + meeting.getTitle() + "' at " + meeting.getSlot());
        }
    }
}
