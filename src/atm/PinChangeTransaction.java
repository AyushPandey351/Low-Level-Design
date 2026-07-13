package atm;

public class PinChangeTransaction extends Transaction {
    private final BankService bankService;
    private final String oldPin;
    private final String newPin;

    public PinChangeTransaction(String transactionId, Account account, BankService bankService,
                                 String oldPin, String newPin) {
        super(transactionId, account, 0.0);
        this.bankService = bankService;
        this.oldPin = oldPin;
        this.newPin = newPin;
    }

    @Override
    public void execute() {
        bankService.changePin(account, oldPin, newPin);
        status = TransactionStatus.SUCCESS;
    }

    @Override
    public void rollback() {
        bankService.changePin(account, newPin, oldPin);
    }
}
