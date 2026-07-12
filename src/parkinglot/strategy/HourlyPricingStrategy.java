package parkinglot.strategy;

import parkinglot.Ticket;
import parkinglot.VehicleType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

// Charges per hour, rounding UP any partial hour (parking for 61 minutes bills as
// 2 hours, not 1.01) - this is how real parking garages bill, and it's also the
// simplest fee model to get wrong: using integer division (minutes / 60) would silently
// undercharge every partial hour, e.g. 90 minutes would bill as 1 hour instead of 2.
public class HourlyPricingStrategy implements PricingStrategy {

    private final Map<VehicleType, Double> ratePerHour;

    public HourlyPricingStrategy(Map<VehicleType, Double> ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        Duration duration = ticket.getParkingDuration(exitTime);
        long minutes = duration.toMinutes();
        long hours = (minutes + 59) / 60; // ceil division, avoids the undercharge bug above
        if (hours == 0) {
            hours = 1; // even a few minutes counts as one billable hour
        }
        double rate = ratePerHour.get(ticket.getVehicle().getType());
        return hours * rate;
    }
}
