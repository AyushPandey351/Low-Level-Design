package parkinglot;

// Deliberately trivial: per the assumptions, we're ignoring the payment gateway
// entirely, so makePayment() just marks itself COMPLETED. The reason this class
// still exists at all (instead of ExitGate just holding a double) is to leave a
// seam: when a real gateway integration is added later, only Payment.makePayment()
// changes (to actually call out to Stripe/Razorpay/etc. and set FAILED on decline) -
// ExitGate and everything upstream stays exactly the same, since they only ever see
// a Payment object with a status, never the mechanics of how it got that status.
public class Payment {
    private final double amount;
    private PaymentStatus status;

    public Payment(double amount) {
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void makePayment() {
        this.status = PaymentStatus.COMPLETED;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
