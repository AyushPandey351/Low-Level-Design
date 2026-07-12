package paymentgateway;

// The core class - represents ONE ATTEMPT to collect money for a PaymentRequest.
// This is deliberately a SEPARATE class from PaymentRequest: a request ("merchant
// wants Rs.1000") can outlive multiple attempts (card declined, retry with UPI) -
// collapsing them into one class would make "how many times did we try to charge
// this order" impossible to represent cleanly.
//
// Note: the property is called `methodType` here (a PaymentType enum value), not
// `PaymentMethod method` as literally written in the notes. Reasoning: Payment is a
// passive record of what happened, not an active participant that invokes pay()
// itself - PaymentProcessor is the one that looks up a PaymentMethod strategy via
// PaymentFactory and calls strategy.pay(payment). If Payment held a live
// PaymentMethod reference, it would blur "who initiates the actual charge," so it
// only stores which type was used, for record-keeping.
//
// Every transition method below is a GUARD CLAUSE against the current status - this
// is what your notes' "Instead of if(status==...) everywhere" is solving without the
// full State pattern: invalid transitions (e.g. capturing a payment that was never
// authorized) throw immediately instead of silently corrupting the payment's history.
public class Payment {
    private final String paymentId;
    private final PaymentRequest request;
    private final PaymentType methodType;
    private double amount;
    private PaymentStatus status;
    private String transactionId;

    public Payment(String paymentId, PaymentRequest request, PaymentType methodType) {
        this.paymentId = paymentId;
        this.request = request;
        this.methodType = methodType;
        this.amount = request.getAmount();
        this.status = PaymentStatus.CREATED;
    }

    // All five transition methods are `synchronized` (on this Payment instance) - this
    // is the fix for the Step 8 "webhook race condition": a status webhook and a
    // client-triggered capture could both call capture() on the SAME Payment at
    // nearly the same instant. requireStatus() is a read-then-write (check status,
    // then assign) - without synchronization, two threads could both pass the check
    // before either writes, both "succeeding" and double-processing the same
    // transition. `synchronized` gives each Payment its own intrinsic lock, so only
    // one transition can run at a time; the second caller simply sees the
    // already-updated status and gets a clean IllegalStateException instead of
    // silently corrupting state. This is the single-JVM, in-memory equivalent of the
    // notes' optimistic-locking/version-field approach - a version field becomes
    // necessary once Payment state lives in a shared database across multiple
    // service instances, since a JVM monitor lock doesn't reach across processes.
    public synchronized void markProcessing() {
        requireStatus(PaymentStatus.CREATED);
        status = PaymentStatus.PROCESSING;
    }

    public synchronized void authorize(String transactionId) {
        requireStatus(PaymentStatus.PROCESSING);
        this.transactionId = transactionId;
        status = PaymentStatus.AUTHORIZED;
    }

    public synchronized void capture() {
        requireStatus(PaymentStatus.AUTHORIZED);
        status = PaymentStatus.CAPTURED;
    }

    public synchronized void fail() {
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Cannot fail a payment that is already " + status);
        }
        status = PaymentStatus.FAILED;
    }

    public synchronized void refund() {
        requireStatus(PaymentStatus.CAPTURED);
        status = PaymentStatus.REFUNDED;
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Payment " + paymentId + " must be " + expected + " but is " + status);
        }
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentRequest getRequest() {
        return request;
    }

    public PaymentType getMethodType() {
        return methodType;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
