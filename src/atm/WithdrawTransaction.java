package atm;

import java.util.Map;

// THE critical correctness class in this whole design - Step 8's "Atomic Transaction"
// requirement made concrete: debit and dispense must never disagree. The order here
// is deliberate: debit FIRST (via BankService, which is really Account's own
// synchronized withdraw()), then attempt to physically dispense. If the debit itself
// fails (insufficient funds), we never even attempt to dispense - nothing to undo.
// If the debit SUCCEEDS but the physical dispense then fails (e.g. the ATM is out of
// the needed notes, even though the account had enough balance), that's the one case
// that actually needs a rollback: we immediately redeposit the same amount before
// reporting failure, so the account is never left short by cash that never came out.
// The alternative failure mode this prevents - cash dispensed but account never
// debited - can't happen either, since dispensing only runs after debit already
// succeeded.
public class WithdrawTransaction extends Transaction {
    private final BankService bankService;
    private final CashDispenser dispenser;
    private final CashDispenseStrategy strategy;

    public WithdrawTransaction(String transactionId, Account account, double amount,
                                BankService bankService, CashDispenser dispenser, CashDispenseStrategy strategy) {
        super(transactionId, account, amount);
        this.bankService = bankService;
        this.dispenser = dispenser;
        this.strategy = strategy;
    }

    @Override
    public void execute() {
        bankService.withdraw(account, amount); // throws InsufficientFundsException if balance too low - nothing dispensed, nothing to roll back

        try {
            Map<Denomination, Integer> breakdown = dispenser.dispenseCash(amount, strategy);
            System.out.println("[WithdrawTransaction] Dispensed: " + breakdown);
            status = TransactionStatus.SUCCESS;
        } catch (IllegalStateException dispenseFailure) {
            rollback();
            status = TransactionStatus.FAILED;
            throw new IllegalStateException(
                    "Cash dispensing failed, transaction rolled back: " + dispenseFailure.getMessage(), dispenseFailure);
        }
    }

    @Override
    public void rollback() {
        bankService.deposit(account, amount);
    }
}
