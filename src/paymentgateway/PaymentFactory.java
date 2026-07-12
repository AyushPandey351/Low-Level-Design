package paymentgateway;

// Centralizes "given a PaymentType, give me the right PaymentMethod strategy" - one
// switch here instead of `if (type == CARD) ... else if (type == UPI) ...` scattered
// through PaymentProcessor and anywhere else a payment gets initiated.
//
// Takes a single GatewayConnector and wires it into every PaymentMethod it creates -
// meaning "which provider are we currently routed through" is decided ONCE, here, not
// per payment type. Your Step 9 improvements list "dynamic gateway routing based on
// success rate/cost" - that's the natural evolution of this class (swap the single
// `connector` field for a routing decision per call), but it's deliberately not built
// now since nothing in the current requirements calls for choosing a DIFFERENT
// provider per request yet.
public class PaymentFactory {
    private final GatewayConnector connector;

    public PaymentFactory(GatewayConnector connector) {
        this.connector = connector;
    }

    // Exposed so PaymentProcessor can route refunds through the same connector used
    // for the original charge, without introducing a second place that wires up
    // GatewayConnector instances.
    public GatewayConnector getConnector() {
        return connector;
    }

    public PaymentMethod getPaymentMethod(PaymentType type) {
        switch (type) {
            case CARD:
                return new CardPayment(connector);
            case UPI:
                return new UPIPayment(connector);
            case WALLET:
                return new WalletPayment(connector);
            case NET_BANKING:
                return new NetBankingPayment(connector);
            default:
                throw new IllegalArgumentException("Unknown payment type: " + type);
        }
    }
}
