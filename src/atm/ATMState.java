package atm;

// Every method has a default that rejects the action - this means each concrete
// state below only needs to override the handful of operations it actually permits,
// instead of every state implementing all seven methods with repetitive "not allowed
// here" boilerplate. This is what replaces the `if (state == ...)` chains your notes
// call out: the ALLOWED-ness of an operation is expressed by whether a given state
// class bothers to override it, not by a conditional anywhere.
public interface ATMState {
    default void insertCard(ATM atm, Card card) {
        throw reject("insert card");
    }

    default void authenticate(ATM atm, String pin) {
        throw reject("authenticate");
    }

    default void withdraw(ATM atm, double amount) {
        throw reject("withdraw");
    }

    default void deposit(ATM atm, double amount) {
        throw reject("deposit");
    }

    default void checkBalance(ATM atm) {
        throw reject("check balance");
    }

    default void changePin(ATM atm, String oldPin, String newPin) {
        throw reject("change PIN");
    }

    default void ejectCard(ATM atm) {
        throw reject("eject card");
    }

    static IllegalStateException reject(String action) {
        return new IllegalStateException("Cannot " + action + " in the current state");
    }
}
