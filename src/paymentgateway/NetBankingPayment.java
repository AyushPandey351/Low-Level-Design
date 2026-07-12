package paymentgateway;

public class NetBankingPayment implements PaymentMethod {
    private final GatewayConnector connector;

    public NetBankingPayment(GatewayConnector connector) {
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
                return new PaymentResult(true, transactionId, "Net banking payment captured");
            }
            payment.fail();
            return new PaymentResult(false, transactionId, "Net banking capture declined");
        } catch (Exception e) {
            payment.fail();
            return new PaymentResult(false, null, "Net banking payment failed: " + e.getMessage());
        }
    }
}
