package bookmyshow;

// This is the class your notes' DIP callout refers to: "Booking depends upon
// PaymentService instead of UPIPayment." Kept deliberately simple and simulated here
// (per the assumption of one payment per booking, no real gateway) rather than
// rebuilding the full Card/UPI/Wallet strategy hierarchy - that hierarchy already
// exists as its own complete design (see the separate paymentgateway package in this
// repo). A production BookMyShow would have THIS class delegate to that system;
// duplicating it here would just be the same Strategy pattern built twice for no
// new teaching value.
//
// The `simulateSuccess` parameter is a pragmatic testing seam - it lets the demo
// deterministically exercise BOTH the happy path (seat gets booked) and the failure
// path (Step 8: "if payment fails, releaseLock()") without needing a real gateway
// that can actually decline a charge.
public class PaymentService {
    public void pay(Payment payment, boolean simulateSuccess) {
        if (simulateSuccess) {
            payment.markSuccess();
        } else {
            payment.markFailed();
        }
    }
}
