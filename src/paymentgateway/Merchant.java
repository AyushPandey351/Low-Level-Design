package paymentgateway;

// Unlike Customer, Merchant DOES get a real method here: receiveWebhook(). The
// difference is that "receiving a webhook" genuinely IS something the merchant's own
// server does in the real world (WebhookService POSTs to merchant.callbackUrl, and
// whatever's listening there is, conceptually, the merchant "receiving" it) - it's not
// logic that belongs to some other coordinator. In this simulated single-process
// design there's no real HTTP call, so this method just represents "what the
// merchant's endpoint would do upon receiving the callback" (e.g. log it, reconcile
// their own order state) - a stand-in for code that would normally live outside this
// system entirely.
public class Merchant {
    private final String merchantId;
    private final String name;
    private final String callbackUrl;

    public Merchant(String merchantId, String name, String callbackUrl) {
        this.merchantId = merchantId;
        this.name = name;
        this.callbackUrl = callbackUrl;
    }

    public void receiveWebhook(Payment payment) {
        System.out.println("[Merchant " + name + " @ " + callbackUrl + "] webhook received: payment "
                + payment.getPaymentId() + " is now " + payment.getStatus());
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getName() {
        return name;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }
}
