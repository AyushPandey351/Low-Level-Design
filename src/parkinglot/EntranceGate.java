package parkinglot;

import java.time.LocalDateTime;

// Thin wrapper around ParkingLot.parkVehicle(). Its whole reason to exist separately
// from ParkingLot is conceptual: in the real world, there can be MULTIPLE physical
// entrance gates feeding into one ParkingLot (that's why it doesn't hold any state of
// its own - it just forwards to the single shared ParkingLot instance). If entry
// logic ever needs gate-specific behavior (e.g. logging which gate issued a ticket),
// this is where it would go without touching ParkingLot itself.
public class EntranceGate {
    private final ParkingLot parkingLot;

    public EntranceGate(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public Ticket issueTicket(Vehicle vehicle, LocalDateTime entryTime) {
        return parkingLot.parkVehicle(vehicle, entryTime);
    }
}
