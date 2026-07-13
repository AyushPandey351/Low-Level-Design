package atm;

// Deliberately empty - relies entirely on ATMState's default reject-everything
// behavior. This state exists to make "a transaction is currently executing" a real,
// distinct, inspectable moment (useful for a UI to show a spinner, or for this demo
// to print atm.getStateName()) - but nothing should be a valid operation WHILE the
// ATM is synchronously busy running a transaction, so there's nothing to override.
public class TransactionSelectedState implements ATMState {
}
