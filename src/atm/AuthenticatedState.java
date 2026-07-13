package atm;

import java.util.function.Supplier;

// Every transaction method here follows the same shape: build the right Transaction
// via TransactionFactory (Factory Pattern), pass through TransactionSelectedState
// while it runs (making "a transaction is in progress" a real, distinct, inspectable
// state rather than an invisible moment), then land in CashDispensedState on
// success or back here on failure - allowing the customer to try a different amount
// or transaction type rather than being kicked back to square one.
public class AuthenticatedState implements ATMState {

    @Override
    public void withdraw(ATM atm, double amount) {
        WithdrawTransaction txn = atm.getTransactionFactory().createWithdraw(
                atm.getCurrentAccount(), amount, atm.getBankService(), atm.getDispenser(), atm.getDispenseStrategy());
        runTransaction(atm, txn, () -> "Please collect your cash.");
    }

    @Override
    public void deposit(ATM atm, double amount) {
        DepositTransaction txn = atm.getTransactionFactory().createDeposit(
                atm.getCurrentAccount(), amount, atm.getBankService());
        runTransaction(atm, txn, () -> "Deposit successful.");
    }

    @Override
    public void checkBalance(ATM atm) {
        BalanceInquiryTransaction txn = atm.getTransactionFactory().createBalanceInquiry(
                atm.getCurrentAccount(), atm.getBankService());
        // The message is a SUPPLIER, evaluated only after runTransaction() has called
        // txn.execute() - reading txn.getResultBalance() any earlier would observe
        // it before execute() ever populated it (it defaults to 0.0 until then).
        runTransaction(atm, txn, () -> "Your balance is " + txn.getResultBalance());
    }

    @Override
    public void changePin(ATM atm, String oldPin, String newPin) {
        PinChangeTransaction txn = atm.getTransactionFactory().createPinChange(
                atm.getCurrentAccount(), atm.getBankService(), oldPin, newPin);
        runTransaction(atm, txn, () -> "PIN changed successfully.");
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.getCardReader().ejectCard();
        atm.setCurrentCard(null);
        atm.setCurrentAccount(null);
        atm.getScreen().displayMessage("Card ejected.");
        atm.setState(new ExitState());
    }

    private void runTransaction(ATM atm, Transaction txn, Supplier<String> successMessage) {
        atm.setLastTransaction(txn);
        atm.setState(new TransactionSelectedState());
        try {
            txn.execute();
        } catch (RuntimeException e) {
            atm.getScreen().displayMessage("Transaction failed: " + e.getMessage());
            atm.setState(new AuthenticatedState());
            throw e;
        }
        atm.getScreen().displayMessage(successMessage.get());
        atm.getReceiptPrinter().printReceipt(txn);
        atm.setState(new CashDispensedState());
    }
}
