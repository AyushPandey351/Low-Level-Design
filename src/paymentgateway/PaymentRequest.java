package paymentgateway;

// "Merchant wants Rs.1000" - a request for money, not the attempt to collect it.
// validate() runs at construction so an invalid PaymentRequest can never exist in the
// first place (fail fast, same philosophy as ExactSplitStrategy validating its sum
// against the total in the Splitwise design) - rejecting bad input here is far cheaper
// than discovering it later inside a half-completed Payment.
public class PaymentRequest {
    private final String requestId;
    private final double amount;
    private final String currency;
    private final String orderId;
    private final Merchant merchant;

    public PaymentRequest(String requestId, double amount, String currency, String orderId, Merchant merchant) {
        this.requestId = requestId;
        this.amount = amount;
        this.currency = currency;
        this.orderId = orderId;
        this.merchant = merchant;
        validate();
    }

    private void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got " + amount);
        }
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("Currency must be specified");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order id must be specified");
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getOrderId() {
        return orderId;
    }

    public Merchant getMerchant() {
        return merchant;
    }
}
