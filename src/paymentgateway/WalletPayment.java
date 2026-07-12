package paymentgateway;

public class WalletPayment implements PaymentMethod {
    private final GatewayConnector connector;

    public WalletPayment(GatewayConnector connector) {
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
                return new PaymentResult(true, transactionId, "Wallet payment captured");
            }
            payment.fail();
            return new PaymentResult(false, transactionId, "Wallet capture declined");
        } catch (Exception e) {
            payment.fail();
            return new PaymentResult(false, null, "Wallet payment failed: " + e.getMessage());
        }
    }
}
