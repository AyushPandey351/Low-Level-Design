package paymentgateway;

import java.util.concurrent.atomic.AtomicInteger;

public class RazorpayConnector implements GatewayConnector {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String authorize(Payment payment) {
        return "RZP_TXN_" + counter.incrementAndGet();
    }

    @Override
    public boolean capture(Payment payment, String transactionId) {
        return true;
    }

    @Override
    public boolean refund(Payment payment, double amount) {
        return true;
    }
}
