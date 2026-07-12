package bookmyshow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BookingFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public Booking createBooking(User user, Show show, List<ShowSeat> seats, double amount) {
        String bookingId = "BKG" + counter.incrementAndGet();
        return new Booking(bookingId, user, show, seats, amount);
    }
}
