package paymentgateway;

import java.util.concurrent.atomic.AtomicInteger;

// Three near-identical implementations on purpose: with real vendor SDKs, these would
// differ a lot (different auth mechanisms, request/response shapes, error codes) -
// but callers of GatewayConnector never see any of that, which is the entire point.
public class PayUConnector implements GatewayConnector {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String authorize(Payment payment) {
        return "PAYU_TXN_" + counter.incrementAndGet();
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
