package bookmyshow;

import java.util.List;
import java.util.Map;

public class NormalPricingStrategy implements PricingStrategy {
    private final Map<SeatType, Double> ratePerSeat;

    public NormalPricingStrategy(Map<SeatType, Double> ratePerSeat) {
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
