package paymentgateway;

// Plain data-holder, same reasoning as User (Splitwise) and Player (Tic Tac Toe):
// initiatePayment() and viewPaymentHistory() are listed as Customer behaviors in the
// notes, but the actual logic - creating a PaymentRequest, querying PaymentRepository -
// needs access to PaymentProcessor/PaymentRepository, which Customer shouldn't hold a
// reference to (same circular-dependency trap as before). PaymentProcessor.createPayment(...)
// and PaymentRepository.find(...) are where that logic actually lives.
public class Customer {
    private final String customerId;
    private final String name;
    private final String email;

    public Customer(String customerId, String name, String email) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name;
    }
}
