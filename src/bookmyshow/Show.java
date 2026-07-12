package bookmyshow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The most important class per your notes. Its constructor is where the Seat/ShowSeat
// split becomes real: it builds a BRAND NEW ShowSeat for every Seat on the Screen,
// so this Show's seating map is entirely independent of any other Show that reuses
// the same physical Screen and Seats.
//
// lockSeats()/confirmBooking() are thin FACADES here, not where the concurrency-
// critical logic lives - they translate the public, Seat-based API into the
// ShowSeat-based calls that SeatLockService actually needs, keeping callers from
// having to know that ShowSeat exists at all. The hard part (preventing two users
// from locking the same seat) is entirely SeatLockService's job.
public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final Map<Seat, ShowSeat> seats = new LinkedHashMap<>();

    public Show(String id, Movie movie, Screen screen, LocalDateTime startTime) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        for (Seat seat : screen.getSeats()) {
            seats.put(seat, new ShowSeat(seat));
        }
    }

    public List<Seat> getAvailableSeats() {
        return seats.values().stream()
                .filter(showSeat -> showSeat.getStatus() == SeatStatus.AVAILABLE)
                .map(ShowSeat::getSeat)
                .collect(Collectors.toList());
    }

    public void lockSeats(List<Seat> requestedSeats, String userId, SeatLockService lockService) {
        List<ShowSeat> showSeats = toShowSeats(requestedSeats);
        lockService.lockSeats(showSeats, userId);
    }

    public void confirmBooking(List<Seat> requestedSeats) {
        for (Seat seat : requestedSeats) {
            getShowSeat(seat).book();
        }
    }

    public List<ShowSeat> toShowSeats(List<Seat> requestedSeats) {
        List<ShowSeat> result = new ArrayList<>();
        for (Seat seat : requestedSeats) {
            result.add(getShowSeat(seat));
        }
        return result;
    }

    private ShowSeat getShowSeat(Seat seat) {
        ShowSeat showSeat = seats.get(seat);
        if (showSeat == null) {
            throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to this show's screen");
        }
        return showSeat;
    }

    public String getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Screen getScreen() {
        return screen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return id + ": " + movie.getName() + " at " + startTime;
    }
}
