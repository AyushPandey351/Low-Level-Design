package parkinglot.strategy;

import parkinglot.Ticket;

import java.time.LocalDateTime;

// Strategy Pattern again, same shape as SplitStrategy in the Splitwise design:
// ParkingLot/ExitGate hold a reference to this INTERFACE, never to a concrete
// HourlyPricingStrategy/WeekendPricingStrategy - that's the DIP callout in your notes.
// Adding a new pricing model (e.g. "first 30 minutes free") later is one new class;
// nothing here or in ExitGate changes.
//
// exitTime is passed in explicitly (not read from the system clock inside the
// strategy) for the same testability reason as Ticket.getParkingDuration().
public interface PricingStrategy {
    double calculateFee(Ticket ticket, LocalDateTime exitTime);
}
