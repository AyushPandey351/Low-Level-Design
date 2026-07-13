package atm;

// rollback() exists for interface symmetry / a future hardware step (e.g. "verify the
// cash-in sensor actually counted the right number of notes" could fail after the
// balance is credited, needing an undo) - not exercised by anything in this design's
// current scope, since a simulated cash deposit has no second step that could fail.
public class DepositTransaction extends Transaction {
    private final BankService bankService;

    public DepositTransaction(String transactionId, Account account, double amount, BankService bankService) {
        super(transactionId, account, amount);
        this.bankService = bankService;
    }

    @Override
    public void execute() {
        bankService.deposit(account, amount);
        status = TransactionStatus.SUCCESS;
    }

    @Override
    public void rollback() {
        bankService.withdraw(account, amount);
        status = TransactionStatus.FAILED;
    }
}
