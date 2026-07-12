package bookmyshow;

import java.util.List;

// Owns the FIXED physical layout of one auditorium - the same List<Seat> is reused
// across every Show ever screened on this Screen. This is what makes Seat having no
// availability flag actually work in practice: the seats here are shared, immutable
// reference data, while each Show builds its OWN independent Map<Seat,ShowSeat> from
// this same list (see Show's constructor).
public class Screen {
    private final String id;
    private final List<Seat> seats;

    public Screen(String id, List<Seat> seats) {
        this.id = id;
        this.seats = seats;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public String getId() {
        return id;
    }
}
