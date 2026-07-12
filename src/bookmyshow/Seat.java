package bookmyshow;

// The favorite interviewer follow-up from your notes, made concrete: THIS class has
// no `boolean booked` or SeatStatus field at all. A Seat represents the PHYSICAL
// chair bolted to the floor of Screen 3, Row F - it exists independent of any
// particular showing. The same physical seat is empty for the 10 AM show, sold out
// for the 6 PM show, and empty again for the 9 PM show, all on the same day.
//
// If availability lived here (`seat.booked = true`), booking it for the 6 PM show
// would make it look booked for EVERY show that ever uses this seat - a single seat
// object shared across all showtimes would need to be reset between shows, which is
// both wrong (what if two shows overlap in time on different screens using seats
// that happen to share an id?) and fragile. Availability is inherently PER-SHOW
// state, so it belongs to ShowSeat (next), not to the seat itself.
public class Seat {
    private final String id;
    private final SeatType type;
    private final int row;
    private final int column;

    public Seat(String id, SeatType type, int row, int column) {
        this.id = id;
        this.type = type;
        this.row = row;
        this.column = column;
    }

    public String getId() {
        return id;
    }

    public SeatType getSeatType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
