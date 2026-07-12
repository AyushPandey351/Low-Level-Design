package paymentgateway;

// Worth being explicit about direction, since it's easy to conflate with
// Merchant.receiveWebhook(): THIS class handles callbacks arriving FROM the external
// gateway/bank INTO our system (e.g. a UPI payment that doesn't confirm synchronously -
// the bank calls us back later to say "actually, this succeeded"). Merchant.receiveWebhook
// is the opposite direction - US notifying the merchant once we know the outcome.
// Two different classes because they're triggered by different parties, carry
// different payloads, and have different trust requirements (this one MUST verify
// the caller is really the bank; outbound notification to our own merchant doesn't).
//
// handleCallback() is also the Step 8 "webhook race condition" fix in this design.
// Payment's transition methods (see Payment.java) are `synchronized` and each checks
// the CURRENT status before transitioning - so if a client-triggered processPayment()
// call and an inbound webhook both try to move the same Payment from AUTHORIZED to
// CAPTURED at nearly the same time, only the first one to acquire the lock succeeds;
// the second sees the already-updated status, its guard clause throws, and we catch
// that here and simply log it as "already applied" rather than letting it corrupt
// state or crash the whole callback handler.
public class WebhookService {
    private final PaymentRepository repository;

    public WebhookService(PaymentRepository repository) {
        this.repository = repository;
    }

    public void handleCallback(String paymentId, PaymentStatus reportedStatus, String signature) {
        if (!verifySignature(paymentId, reportedStatus, signature)) {
            System.out.println("Webhook rejected for payment " + paymentId + ": invalid signature");
            return;
        }

        Payment payment = repository.find(paymentId);
        if (payment == null) {
            System.out.println("Webhook for unknown payment " + paymentId + " ignored");
            return;
        }

        try {
            if (reportedStatus == PaymentStatus.CAPTURED) {
                payment.capture();
            } else if (reportedStatus == PaymentStatus.FAILED) {
                payment.fail();
            }
            repository.update(payment);
            System.out.println("Webhook applied: payment " + paymentId + " -> " + payment.getStatus());
        } catch (IllegalStateException e) {
            System.out.println("Webhook for payment " + paymentId + " ignored (already " + payment.getStatus() + ")");
        }
    }

    // Simulated: a real implementation would HMAC-verify the payload against a secret
    // shared with the provider, so a forged request hitting our public webhook
    // endpoint directly (without ever touching the real bank) can't be used to mark
    // arbitrary payments as CAPTURED.
    private boolean verifySignature(String paymentId, PaymentStatus reportedStatus, String signature) {
        return signature != null && signature.startsWith("valid_");
    }
}
