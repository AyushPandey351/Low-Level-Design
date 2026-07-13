package atm;

// Every money-mutating method here is `synchronized` on the Account instance itself -
// this IS "Solution 1: Account Lock" from your Step 8 notes, implemented via Java's
// intrinsic per-object monitor rather than an explicit external lock map. The bug your
// notes describe (ATM A and ATM B both read balance=1000, both then withdraw, final
// balance goes negative) happens specifically because "read balance, check sufficient,
// then write new balance" is three separate steps - if two threads can interleave
// between them, both can pass the check before either writes. Making withdraw() one
// synchronized method means the ENTIRE read-check-write sequence is atomic: the
// second concurrent caller can't even begin until the first has fully committed its
// write, so it sees the ALREADY-UPDATED balance, not the stale one.
//
// checkBalance() is ALSO synchronized, which is easy to skip since it looks read-only
// and harmless - but per the Java Language Spec, reads/writes of a plain (non-volatile)
// double field are not guaranteed atomic. Without synchronization, a concurrent reader
// could theoretically observe a "torn" value while a write is in progress. Since this
// method guards money, it's not worth relying on "usually fine in practice."
//
// This intrinsic-lock approach is the right level of complexity for a single-JVM,
// in-memory bank simulation. The notes' alternative - optimistic locking with a
// version field, retrying on conflict - becomes the better choice once Account state
// lives in a shared database accessed by multiple independent service instances that
// don't share JVM memory, where a monitor lock can't reach across processes.
public class Account {
    private final String accountNumber;
    private double balance;
    private String pin;

    public Account(String accountNumber, double balance, String pin) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.pin = pin;
    }

    public synchronized void withdraw(double amount) {
        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Account " + accountNumber + " has insufficient funds for withdrawal of " + amount);
        }
        balance -= amount;
    }

    public synchronized void deposit(double amount) {
        balance += amount;
    }

    public synchronized double checkBalance() {
        return balance;
    }

    public synchronized void changePin(String oldPin, String newPin) {
        if (!pin.equals(oldPin)) {
            throw new IllegalArgumentException("Incorrect current PIN");
        }
        pin = newPin;
    }

    public boolean isPinCorrect(String candidatePin) {
        return pin.equals(candidatePin);
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
