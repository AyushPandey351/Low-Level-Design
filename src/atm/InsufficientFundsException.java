package atm;

// A distinct type from a generic IllegalStateException because "insufficient funds"
// is an expected, specific business outcome a caller might want to catch and handle
// differently from other failures (e.g. show "insufficient balance" vs. a generic
// error screen) - not just an invariant violation.
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
