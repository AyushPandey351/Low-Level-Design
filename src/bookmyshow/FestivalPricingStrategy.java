package bookmyshow;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// Same wrapping technique again, this time keyed off a configured set of festival
// dates rather than day-of-week - the POINT being that the wrapping pattern itself
// doesn't care what condition triggers the surcharge, only that it can ask "does
// this show's date qualify" and multiply the base fee if so.
public class FestivalPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    private final Set<LocalDate> festivalDates;
    private final double festivalMultiplier;

    public FestivalPricingStrategy(PricingStrategy baseStrategy, Set<LocalDate> festivalDates, double festivalMultiplier) {
        this.baseStrategy = baseStrategy;
        this.festivalDates = festivalDates;
        this.festivalMultiplier = festivalMultiplier;
    }

    @Override
    public double calculateFee(Show show, List<Seat> seats) {
        double baseFee = baseStrategy.calculateFee(show, seats);
        LocalDate showDate = show.getStartTime().toLocalDate();
        return festivalDates.contains(showDate) ? baseFee * festivalMultiplier : baseFee;
    }
}
