package atm;

public class ReceiptPrinter {
    public void printReceipt(Transaction transaction) {
        System.out.println("[Receipt] ------------------------------");
        System.out.println("[Receipt] Transaction: " + transaction.getTransactionId());
        System.out.println("[Receipt] Account:     " + transaction.getAccount().getAccountNumber());
        System.out.println("[Receipt] Amount:      " + transaction.getAmount());
        System.out.println("[Receipt] Status:      " + transaction.getStatus());
        System.out.println("[Receipt] Time:        " + transaction.getTime());
        System.out.println("[Receipt] ------------------------------");
    }
}
