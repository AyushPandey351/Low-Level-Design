package paymentgateway;

// Strategy Pattern, same shape as every prior *Strategy interface (SplitStrategy,
// PricingStrategy, WinningStrategy). PaymentProcessor holds this INTERFACE, never a
// concrete CardPayment/UPIPayment - that's the DIP callout in your notes, and it's
// what makes adding Apple Pay/Google Pay/Crypto later a one-class change with zero
// edits to PaymentProcessor.
//
// pay() is what actually drives Payment through its state transitions
// (markProcessing -> authorize -> capture), because each implementation is the one
// that knows, step by step, what the GatewayConnector calls returned - PaymentProcessor
// only needs the final PaymentResult, not the blow-by-blow gateway interaction.
public interface PaymentMethod {
    PaymentResult pay(Payment payment);
}
