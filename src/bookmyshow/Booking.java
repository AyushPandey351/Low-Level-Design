package bookmyshow;

import java.util.List;

// Represents one booking transaction. Holds List<ShowSeat> directly (per your notes'
// property list) rather than List<Seat> - since Booking's whole job is to flip THIS
// show's seats through their lifecycle, it needs the ShowSeat references, not just
// the underlying physical Seats.
//
// confirm()/cancel()/fail() mirror the guarded-transition discipline used throughout
// this series (Payment in Payment Gateway, Meeting in Meeting Scheduler): CREATED is
// the only state confirm() or fail() may start from, and CONFIRMED is the only state
// cancel() may start from. Both confirm() and fail()/cancel() drive the underlying
// ShowSeats through the matching transition (book() or release()) as part of the
// SAME call - so from the caller's point of view, "confirm this booking" and "the
// seats become BOOKED" can never happen as two separately-forgettable steps.
public class Booking {
    private final String bookingId;
    private final User user;
    private final Show show;
    private final List<ShowSeat> seats;
    private final double amount;
    private BookingStatus status;

    public Booking(String bookingId, User user, Show show, List<ShowSeat> seats, double amount) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.amount = amount;
        this.status = BookingStatus.CREATED;
    }

    public void confirm() {
        requireStatus(BookingStatus.CREATED);
        for (ShowSeat seat : seats) {
            seat.book();
            SeatLockService.getInstance().markBooked(seat);
        }
        status = BookingStatus.CONFIRMED;
    }

    // Payment never went through - release the held locks so the seats go back on
    // sale immediately, rather than making other users wait out the full lock timeout.
    public void fail() {
        requireStatus(BookingStatus.CREATED);
        for (ShowSeat seat : seats) {
            SeatLockService.getInstance().unlock(seat, user.getUserId());
        }
        status = BookingStatus.FAILED;
    }

    // A previously CONFIRMED (paid) booking is being cancelled - the seats were
    // BOOKED, not LOCKED, so this goes through ShowSeat.release() directly rather
    // than SeatLockService.unlock() (which only knows about LOCKED seats it's tracking).
    public void cancel() {
        requireStatus(BookingStatus.CONFIRMED);
        for (ShowSeat seat : seats) {
            seat.release();
        }
        status = BookingStatus.CANCELLED;
    }

    private void requireStatus(BookingStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Booking " + bookingId + " must be " + expected + " but is " + status);
        }
    }

    public String getBookingId() {
        return bookingId;
    }

    public User getUser() {
        return user;
    }

    public Show getShow() {
        return show;
    }

    public List<ShowSeat> getSeats() {
        return seats;
    }

    public double getAmount() {
        return amount;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
