package parkinglot;

import java.time.Duration;
import java.time.LocalDateTime;

// Represents one parking session, from entry until it's discarded after payment.
// Immutable, same reasoning as Expense in the Splitwise design: a Ticket is a record
// of something that already happened (a vehicle entered at this time, into this spot),
// so nothing about it should be editable after the fact.
//
// getParkingDuration() takes exitTime as a PARAMETER rather than calling
// LocalDateTime.now() internally. This matters for two reasons: (1) it keeps Ticket
// pure/deterministic - calling it twice with the same exitTime always gives the same
// answer, which makes it trivially testable, and (2) the actual exit time is a fact
// ExitGate/PricingStrategy already knows when they call this - there's no reason to
// make Ticket reach out to the system clock itself when the caller can just hand it in.
public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;

    public Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public Duration getParkingDuration(LocalDateTime exitTime) {
        return Duration.between(entryTime, exitTime);
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }
}
