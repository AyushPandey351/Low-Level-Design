package bookmyshow;

import java.time.DayOfWeek;
import java.util.List;

// Third time this exact composition technique shows up in this series (after
// WeekendPricingStrategy in Parking Lot and the fallback pattern in
// PreferredSlotStrategy for Meeting Scheduler): wrap a base strategy and apply a
// surcharge multiplier conditionally, rather than duplicating a full rate table.
// "Weekend" isn't a different pricing MODEL here either - it's a modifier on
// whatever base model is already configured.
public class WeekendPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    private final double weekendMultiplier;

    public WeekendPricingStrategy(PricingStrategy baseStrategy, double weekendMultiplier) {
        this.baseStrategy = baseStrategy;
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override
    public double calculateFee(Show show, List<Seat> seats) {
        double baseFee = baseStrategy.calculateFee(show, seats);
        DayOfWeek day = show.getStartTime().getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return isWeekend ? baseFee * weekendMultiplier : baseFee;
    }
}
