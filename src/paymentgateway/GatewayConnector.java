package paymentgateway;

// This is a SECOND, independent Strategy layer, distinct from PaymentMethod. Easy to
// conflate the two, so worth being explicit: PaymentMethod (next) answers "what KIND
// of payment is this - card, UPI, wallet?" GatewayConnector answers "which EXTERNAL
// PROVIDER actually moves the money - Razorpay, Stripe, PayU?" A single CardPayment
// could route through any of these three connectors; a single RazorpayConnector could
// be used by CardPayment, UPIPayment, or WalletPayment alike. Collapsing both
// decisions into one class hierarchy (e.g. "RazorpayCardPayment", "StripeUPIPayment")
// would multiply combinatorially with every new method x every new provider - keeping
// them as two separate, independently swappable strategies avoids that explosion.
//
// In our simulated design (no real bank/network involved, per the assumptions),
// each implementation just fabricates a transaction id and returns success - standing
// in for what would otherwise be a real HTTP call to that provider's API.
public interface GatewayConnector {
    String authorize(Payment payment);
    boolean capture(Payment payment, String transactionId);
    boolean refund(Payment payment, double amount);
}
