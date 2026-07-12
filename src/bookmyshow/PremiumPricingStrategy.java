package bookmyshow;

import java.util.List;
import java.util.Map;

// Unlike Weekend/Festival, this ISN'T a wrapper - it's a genuinely different base
// rate table, for theatres/shows (e.g. a premiere) that price every seat type higher
// across the board rather than applying a conditional surcharge on top of the normal
// rates. Included to show both flavors of extending PricingStrategy are equally
// valid: a brand new rate table, or a modifier composed onto an existing one.
public class PremiumPricingStrategy implements PricingStrategy {
    private final Map<SeatType, Double> ratePerSeat;

    public PremiumPricingStrategy(Map<SeatType, Double> ratePerSeat) {
        this.ratePerSeat = ratePerSeat;
    }

    @Override
    public double calculateFee(Show show, List<Seat> seats) {
        double total = 0;
        for (Seat seat : seats) {
            total += ratePerSeat.get(seat.getSeatType());
        }
        return total;
    }
}
