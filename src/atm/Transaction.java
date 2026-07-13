package atm;

import java.time.Instant;

// Base class for every transaction type - LSP in action: AuthenticatedState (next)
// works with whichever concrete Transaction its factory hands back, calling only
// execute(), without needing to know or care which subtype it actually got.
//
// rollback() is a real, separately-callable method (not just inlined compensating
// logic inside execute()) so each subtype can define what "undo" means for itself -
// for WithdrawTransaction that's redepositing the debited amount; for the read-only
// transactions (BalanceInquiry, PinChange) it's a no-op, since there's no money
// movement to reverse. Keeping it as its own method (rather than folding the
// redeposit directly into WithdrawTransaction.execute()'s catch block) is what makes
// it independently testable and consistent with your notes' explicit class design.
public abstract class Transaction {
    protected final String transactionId;
    protected final Instant time;
    protected final Account account;
    protected final double amount;
    protected TransactionStatus status;

    protected Transaction(String transactionId, Account account, double amount) {
        this.transactionId = transactionId;
        this.time = Instant.now();
        this.account = account;
        this.amount = amount;
        this.status = TransactionStatus.PENDING;
    }

    public abstract void execute();

    public abstract void rollback();

    public String getTransactionId() {
        return transactionId;
    }

    public Instant getTime() {
        return time;
    }

    public Account getAccount() {
        return account;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }
}
