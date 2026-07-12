package paymentgateway;

import java.util.concurrent.atomic.AtomicInteger;

// Singleton, same initialize()/getInstance() split as ParkingLot, and for the same
// reason: this needs configured collaborators (a PaymentFactory, a PaymentRepository)
// at creation time, so a lazily-self-constructing no-arg getInstance() doesn't fit.
//
// createPayment() is where the idempotency fix from your Step 8 notes actually runs.
// The order of operations matters: we generate a candidate id and attempt to RESERVE
// the idempotency key FIRST - before creating any real PaymentRequest/Payment or
// touching a gateway - because reserving is cheap and side-effect-free, while
// actually charging is not. If reserveIdempotencyKey() tells us someone else already
// owns this key (the other half of a double-click race), we return their existing
// Payment immediately and never construct a second one - so the double-click never
// results in a second charge, no matter how close together the two calls land.
public class PaymentProcessor {
    private static PaymentProcessor instance;

    private final PaymentFactory factory;
    private final PaymentRepository repository;
    private final AtomicInteger idCounter = new AtomicInteger();

    private PaymentProcessor(PaymentFactory factory, PaymentRepository repository) {
        this.factory = factory;
        this.repository = repository;
    }

    public static synchronized PaymentProcessor initialize(PaymentFactory factory, PaymentRepository repository) {
        if (instance != null) {
            throw new IllegalStateException("PaymentProcessor is already initialized");
        }
        instance = new PaymentProcessor(factory, repository);
        return instance;
    }

    public static synchronized PaymentProcessor getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PaymentProcessor has not been initialized");
        }
        return instance;
    }

    public Payment createPayment(Merchant merchant, double amount, String currency, String orderId,
                                  PaymentType methodType, String idempotencyKey) {
        String candidateId = "PAY" + idCounter.incrementAndGet();

        String existingPaymentId = repository.reserveIdempotencyKey(idempotencyKey, candidateId);
        if (existingPaymentId != null) {
            System.out.println("Idempotency key '" + idempotencyKey
                    + "' already used - returning existing payment " + existingPaymentId + ", NOT charging again");
            return repository.find(existingPaymentId);
        }

        PaymentRequest request = new PaymentRequest("REQ_" + candidateId, amount, currency, orderId, merchant);
        Payment payment = new Payment(candidateId, request, methodType);
        repository.save(payment);
        return payment;
    }

    public PaymentResult processPayment(String paymentId) {
        Payment payment = repository.find(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("No such payment: " + paymentId);
        }

        PaymentMethod paymentMethod = factory.getPaymentMethod(payment.getMethodType());
        PaymentResult result = paymentMethod.pay(payment);
        repository.update(payment);

        if (result.isSuccess()) {
            payment.getRequest().getMerchant().receiveWebhook(payment);
        }
        return result;
    }

    public PaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = repository.find(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("No such payment: " + paymentId);
        }
        return payment.getStatus();
    }

    public Refund refund(String paymentId, double amount) {
        Payment payment = repository.find(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("No such payment: " + paymentId);
        }
        Refund refund = new Refund("REF" + idCounter.incrementAndGet(), payment, amount);
        refund.processRefund(factory.getConnector());
        repository.update(payment);
        return refund;
    }

    public PaymentRepository getRepository() {
        return repository;
    }
}
