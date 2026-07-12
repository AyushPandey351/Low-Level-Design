package meetingscheduler;

// Why not just store User directly in Meeting.participants? Because acceptance state
// is PER-MEETING, not per-user: the same User is ACCEPTED on one meeting and PENDING
// on another simultaneously. If Meeting stored List<User>, there'd be nowhere to put
// that per-meeting response status without bolting a parallel Map<User,InvitationStatus>
// onto Meeting - Participant just merges the two into one cohesive object.
public class Participant {
    private final User user;
    private InvitationStatus status;

    public Participant(User user, InvitationStatus status) {
        this.user = user;
        this.status = status;
    }

    public void accept() {
        status = InvitationStatus.ACCEPTED;
    }

    public void decline() {
        status = InvitationStatus.DECLINED;
    }

    public void tentative() {
        status = InvitationStatus.TENTATIVE;
    }

    public User getUser() {
        return user;
    }

    public InvitationStatus getStatus() {
        return status;
    }
}
