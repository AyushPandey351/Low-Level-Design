package paymentgateway;

// Identical shape to CardPayment - same reasoning as the three GatewayConnector
// implementations: this is what makes them genuinely interchangeable (LSP) from
// PaymentProcessor's point of view, even though the real-world mechanics of a UPI
// collect request vs. a card auth are quite different.
public class UPIPayment implements PaymentMethod {
    private final GatewayConnector connector;

    public UPIPayment(GatewayConnector connector) {
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
                return new PaymentResult(true, transactionId, "UPI payment captured");
            }
            payment.fail();
            return new PaymentResult(false, transactionId, "UPI capture declined");
        } catch (Exception e) {
            payment.fail();
            return new PaymentResult(false, null, "UPI payment failed: " + e.getMessage());
        }
    }
}
