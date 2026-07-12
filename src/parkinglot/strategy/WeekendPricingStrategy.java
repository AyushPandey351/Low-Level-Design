package parkinglot.strategy;

import parkinglot.Ticket;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

// Rather than duplicating rate tables, WeekendPricingStrategy WRAPS another
// PricingStrategy (any of them - Hourly, FlatRate, even another Weekend) and applies
// a surcharge multiplier on top if the vehicle entered on a Saturday/Sunday. This is
// composition instead of inheritance: "weekend pricing" isn't really a different fee
// MODEL, it's a modifier layered on top of whatever base model is already in use.
// It still satisfies the PricingStrategy interface, so ParkingLot/ExitGate can hold
// it exactly like any other strategy without knowing it's a wrapper.
public class WeekendPricingStrategy implements PricingStrategy {

    private final PricingStrategy baseStrategy;
    private final double weekendMultiplier;

    public WeekendPricingStrategy(PricingStrategy baseStrategy, double weekendMultiplier) {
        this.baseStrategy = baseStrategy;
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        double baseFee = baseStrategy.calculateFee(ticket, exitTime);
        DayOfWeek entryDay = ticket.getEntryTime().getDayOfWeek();
        boolean isWeekend = entryDay == DayOfWeek.SATURDAY || entryDay == DayOfWeek.SUNDAY;
        return isWeekend ? baseFee * weekendMultiplier : baseFee;
    }
}
