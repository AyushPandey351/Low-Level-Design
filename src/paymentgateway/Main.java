package paymentgateway;

public class Main {
    public static void main(String[] args) {
        PaymentRepository repository = new PaymentRepository();
        GatewayConnector connector = new RazorpayConnector();
        PaymentFactory factory = new PaymentFactory(connector);
        PaymentProcessor processor = PaymentProcessor.initialize(factory, repository);
        WebhookService webhookService = new WebhookService(repository);

        Merchant merchant = new Merchant("M1", "Ayush's Store", "https://ayushstore.example.com/webhook");
        Customer customer = new Customer("C1", "Rahul", "rahul@example.com");

        // --- Customer pays via Card ---
        System.out.println("== " + customer + " pays " + merchant.getName() + " Rs.1000 via CARD ==");
        Payment payment1 = processor.createPayment(merchant, 1000.0, "INR", "ORDER1", PaymentType.CARD, "idem-key-1");
        System.out.println("Payment created: " + payment1.getPaymentId() + ", status=" + payment1.getStatus());
        PaymentResult result1 = processor.processPayment(payment1.getPaymentId());
        System.out.println("Result: success=" + result1.isSuccess() + ", txn=" + result1.getTransactionId());
        System.out.println("Final status: " + processor.getPaymentStatus(payment1.getPaymentId()));

        // --- Customer double-clicks "Pay Now" with the SAME idempotency key ---
        System.out.println("\n== Double-click: same idempotency key 'idem-key-1' sent again ==");
        Payment duplicateAttempt = processor.createPayment(merchant, 1000.0, "INR", "ORDER1", PaymentType.CARD, "idem-key-1");
        System.out.println("Returned payment id: " + duplicateAttempt.getPaymentId()
                + " (same as original: " + duplicateAttempt.getPaymentId().equals(payment1.getPaymentId()) + ")");

        // --- Refund the original payment ---
        System.out.println("\n== Refunding payment " + payment1.getPaymentId() + " ==");
        Refund refund = processor.refund(payment1.getPaymentId(), 1000.0);
        System.out.println("Refund " + refund.getRefundId() + " status: " + refund.getStatus());
        System.out.println("Payment status after refund: " + processor.getPaymentStatus(payment1.getPaymentId()));

        // --- A second, independent payment via UPI ---
        System.out.println("\n== A second customer pays via UPI ==");
        Payment payment2 = processor.createPayment(merchant, 500.0, "INR", "ORDER2", PaymentType.UPI, "idem-key-2");
        processor.processPayment(payment2.getPaymentId());
        System.out.println("Payment " + payment2.getPaymentId() + " status: " + processor.getPaymentStatus(payment2.getPaymentId()));

        // --- Webhook race: the gateway ALSO sends an async callback for the same payment ---
        // (already CAPTURED via the synchronous flow above) - this must be safely ignored,
        // not double-applied or crash the handler.
        System.out.println("\n== Late webhook arrives for payment " + payment2.getPaymentId() + " (already CAPTURED) ==");
        webhookService.handleCallback(payment2.getPaymentId(), PaymentStatus.CAPTURED, "valid_signature_abc");

        // --- A forged webhook (bad signature) must be rejected outright ---
        System.out.println("\n== Forged webhook with invalid signature ==");
        webhookService.handleCallback(payment2.getPaymentId(), PaymentStatus.FAILED, "forged_signature");
        System.out.println("Payment " + payment2.getPaymentId() + " status unaffected: "
                + processor.getPaymentStatus(payment2.getPaymentId()));
    }
}
