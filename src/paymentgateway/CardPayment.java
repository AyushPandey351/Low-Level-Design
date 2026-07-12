package paymentgateway;

// Drives Payment through CREATED -> PROCESSING -> AUTHORIZED -> CAPTURED (or FAILED)
// step by step, using an injected GatewayConnector to actually talk to the provider.
// The connector is injected (constructor parameter), not hardcoded to
// `new RazorpayConnector()` - so the SAME CardPayment class works whether it's routed
// through Razorpay, Stripe, or PayU. This is what makes the two Strategy layers
// (PaymentMethod x GatewayConnector) genuinely independent: swapping providers never
// requires touching CardPayment, UPIPayment, etc.
//
// try/catch around the whole flow ensures a gateway exception can never leave a
// Payment stuck mid-transition (e.g. AUTHORIZED forever, neither captured nor
// failed) - every code path ends in either a CAPTURED result or an explicit fail().
public class CardPayment implements PaymentMethod {
    private final GatewayConnector connector;

    public CardPayment(GatewayConnector connector) {
        this.connector = connector;
    }

    @Override
    public PaymentResult pay(Payment payment) {
        payment.markProcessing();
        try {
            String transactionId = connector.authorize(payment);
            payment.authorize(transactionId);

            boolean captured = connector.capture(payment, transactionId);
            if (captured) {
                payment.capture();
                return new PaymentResult(true, transactionId, "Card payment captured");
            }
            payment.fail();
            return new PaymentResult(false, transactionId, "Card capture declined");
        } catch (Exception e) {
            payment.fail();
            return new PaymentResult(false, null, "Card payment failed: " + e.getMessage());
        }
    }
}
