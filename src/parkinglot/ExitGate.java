package parkinglot;

import java.time.LocalDateTime;

// Orchestrates the exit flow from your Step 5 API: unpark -> calculateFee -> payment
// -> releaseSpot. Each step is also exposed as its own method (matching your notes'
// acceptTicket/calculateAmount/makePayment/releaseSpot) so callers CAN drive the flow
// step by step if needed (e.g. show the fee to the driver before charging them) - but
// processExit() bundles them in the correct order for the common case, so callers
// don't have to remember (and can't get wrong) the sequence themselves.
//
// Crucially, releaseSpot only runs AFTER payment succeeds - releasing the spot before
// payment would let the vehicle leave without ever being charged, and releasing it
// unconditionally (regardless of payment result) would do the same the moment a real
// payment gateway (see Payment's comment) starts returning FAILED sometimes.
public class ExitGate {
    private final ParkingLot parkingLot;

    public ExitGate(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public Ticket acceptTicket(Ticket ticket) {
        return ticket;
    }

    public double calculateAmount(Ticket ticket, LocalDateTime exitTime) {
        return parkingLot.getPricingStrategy().calculateFee(ticket, exitTime);
    }

    public Payment makePayment(double amount) {
        Payment payment = new Payment(amount);
        payment.makePayment();
        return payment;
    }

    public void releaseSpot(Ticket ticket) {
        parkingLot.unparkVehicle(ticket);
    }

    public Payment processExit(Ticket ticket, LocalDateTime exitTime) {
        acceptTicket(ticket);
        double amount = calculateAmount(ticket, exitTime);
        Payment payment = makePayment(amount);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            releaseSpot(ticket);
        }
        return payment;
    }
}
