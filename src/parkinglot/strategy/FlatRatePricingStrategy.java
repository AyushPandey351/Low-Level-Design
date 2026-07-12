package parkinglot.strategy;

import parkinglot.Ticket;

import java.time.LocalDateTime;

// The simplest possible strategy: same fee no matter how long the vehicle stayed.
// Included mainly to prove the interface is genuinely flexible - a strategy doesn't
// even have to look at duration at all, since Ticket/exitTime are just available to
// it, not required to be used.
public class FlatRatePricingStrategy implements PricingStrategy {

    private final double flatFee;

    public FlatRatePricingStrategy(double flatFee) {
        this.flatFee = flatFee;
    }

    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        return flatFee;
    }
}
