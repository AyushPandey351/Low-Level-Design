package bookmyshow;

import java.util.List;

// Strategy Pattern, same shape as every prior *Strategy in this series. Booking
// (indirectly, via whichever strategy the caller picks) depends on this interface,
// never a concrete NormalPricingStrategy/WeekendPricingStrategy - adding
// FestivalPricingStrategy or a future "opening weekend premium" tier is a one-class
// addition, no existing code changes.
public interface PricingStrategy {
    double calculateFee(Show show, List<Seat> seats);
}
