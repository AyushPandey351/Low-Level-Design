package paymentgateway;

// Mirrors the PaymentRequest/Payment split: Refund is "the attempt to give money
// back," tracked separately from Payment's own status flip. processRefund() calls
// out to the gateway, and only on success does it flip the underlying Payment from
// CAPTURED to REFUNDED (via Payment's own guarded, synchronized refund() method) -
// Refund never mutates Payment's status field directly, it goes through the same
// guarded API everything else does.
public class Refund {
    private final String refundId;
    private final Payment payment;
    private final double amount;
    private RefundStatus status;

    public Refund(String refundId, Payment payment, double amount) {
        this.refundId = refundId;
        this.payment = payment;
        this.amount = amount;
        this.status = RefundStatus.INITIATED;
    }

    public void processRefund(GatewayConnector connector) {
        boolean success = connector.refund(payment, amount);
        if (success) {
            payment.refund();
            status = RefundStatus.PROCESSED;
        } else {
            status = RefundStatus.FAILED;
        }
    }

    public String getRefundId() {
        return refundId;
    }

    public Payment getPayment() {
        return payment;
    }

    public double getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }
}
