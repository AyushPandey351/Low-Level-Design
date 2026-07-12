package bookmyshow;

// THIS is where per-show availability actually lives, per Seat's comment. Every Show
// builds a fresh ShowSeat for each physical Seat on its Screen, so the same Seat
// produces completely independent ShowSeat objects (and therefore independent
// status) across different showtimes.
//
// lock()/book()/release() are `synchronized` guarded transitions - same discipline as
// Payment's state transitions in the Payment Gateway design. But note what these
// guards do NOT do: they don't implement "only one of two simultaneous callers wins."
// If two threads call lock() on the same ShowSeat at once, the synchronized guard
// does ensure only one succeeds (the other sees status != AVAILABLE and throws) - so
// technically this alone prevents double-booking a single seat. What ShowSeat can't
// do is track WHO holds the lock or WHEN it expires - that bookkeeping is exactly
// why SeatLockService (next) exists as a separate class, per your notes calling it
// out as a favorite interviewer class: it's not just about preventing the race, it's
// about owning lock OWNERSHIP and TIMEOUT, which is state that doesn't belong on the
// seat itself.
public class ShowSeat {
    private final Seat seat;
    private SeatStatus status;

    public ShowSeat(Seat seat) {
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }

    public synchronized void lock() {
        requireStatus(SeatStatus.AVAILABLE);
        status = SeatStatus.LOCKED;
    }

    public synchronized void book() {
        requireStatus(SeatStatus.LOCKED);
        status = SeatStatus.BOOKED;
    }

    // Valid from LOCKED (payment failed / lock expired) or BOOKED (a confirmed
    // booking was cancelled and the seat goes back on sale) - both cases end the
    // same way, the seat becomes available again.
    public synchronized void release() {
        if (status == SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat " + seat.getId() + " is already available");
        }
        status = SeatStatus.AVAILABLE;
    }

    private void requireStatus(SeatStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Seat " + seat.getId() + " must be " + expected + " but is " + status);
        }
    }

    public Seat getSeat() {
        return seat;
    }

    public SeatStatus getStatus() {
        return status;
    }
}
