package atm;

// A read-only transaction - execute() never mutates money, so rollback() is a
// legitimate no-op, not a missing implementation. Still modeled as a full
// Transaction subtype (rather than a special-cased method elsewhere) so it can flow
// through the exact same execute()/receipt/state-transition pipeline as every other
// transaction type - that uniformity is the entire payoff of the Transaction
// abstraction from an OCP standpoint.
public class BalanceInquiryTransaction extends Transaction {
    private final BankService bankService;
    private double resultBalance;

    public BalanceInquiryTransaction(String transactionId, Account account, BankService bankService) {
        super(transactionId, account, 0.0);
        this.bankService = bankService;
    }

    @Override
    public void execute() {
        resultBalance = bankService.checkBalance(account);
        status = TransactionStatus.SUCCESS;
    }

    @Override
    public void rollback() {
        // Nothing to undo - balance inquiries never mutate account state.
    }

    public double getResultBalance() {
        return resultBalance;
    }
}
