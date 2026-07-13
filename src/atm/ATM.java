package atm;

// The coordinator, per your notes. Every public method here is a ONE-LINE delegation
// to currentState - there is NOT ONE `if (state == ...)` anywhere in this class. That's
// the entire payoff of the State pattern: ATM doesn't know or care what's currently
// allowed, it just asks the current state object, and the state object either
// performs the action (and transitions ATM to the next state) or rejects it via
// ATMState's default reject behavior.
//
// Holds all the hardware/service collaborators as fields so state classes can reach
// them via package-private getters - state classes live in the SAME package
// deliberately, so they can access ATM's internals without needing public setters
// exposed to arbitrary outside callers.
public class ATM {
    private final String atmId;
    private ATMState currentState;

    private final CardReader cardReader;
    private final Screen screen;
    private final Keypad keypad;
    private final CashDispenser dispenser;
    private final BankService bankService;
    private final ReceiptPrinter receiptPrinter;
    private final TransactionFactory transactionFactory;
    private final CashDispenseStrategy dispenseStrategy;

    private Card currentCard;
    private Account currentAccount;
    private Transaction lastTransaction;

    public ATM(String atmId, CardReader cardReader, Screen screen, Keypad keypad, CashDispenser dispenser,
               BankService bankService, ReceiptPrinter receiptPrinter, TransactionFactory transactionFactory,
               CashDispenseStrategy dispenseStrategy) {
        this.atmId = atmId;
        this.cardReader = cardReader;
        this.screen = screen;
        this.keypad = keypad;
        this.dispenser = dispenser;
        this.bankService = bankService;
        this.receiptPrinter = receiptPrinter;
        this.transactionFactory = transactionFactory;
        this.dispenseStrategy = dispenseStrategy;
        this.currentState = new IdleState();
    }

    // --- Public API, matching Step 5 - pure delegation, no branching ---
    public void insertCard(Card card) {
        currentState.insertCard(this, card);
    }

    public void authenticate(String pin) {
        currentState.authenticate(this, pin);
    }

    public void withdraw(double amount) {
        currentState.withdraw(this, amount);
    }

    public void deposit(double amount) {
        currentState.deposit(this, amount);
    }

    public void checkBalance() {
        currentState.checkBalance(this);
    }

    public void changePin(String oldPin, String newPin) {
        currentState.changePin(this, oldPin, newPin);
    }

    public void ejectCard() {
        currentState.ejectCard(this);
    }

    // --- Package-private accessors for state classes ---
    void setState(ATMState state) {
        this.currentState = state;
    }

    ATMState getState() {
        return currentState;
    }

    CardReader getCardReader() {
        return cardReader;
    }

    Screen getScreen() {
        return screen;
    }

    Keypad getKeypad() {
        return keypad;
    }

    CashDispenser getDispenser() {
        return dispenser;
    }

    BankService getBankService() {
        return bankService;
    }

    ReceiptPrinter getReceiptPrinter() {
        return receiptPrinter;
    }

    TransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    CashDispenseStrategy getDispenseStrategy() {
        return dispenseStrategy;
    }

    Card getCurrentCard() {
        return currentCard;
    }

    void setCurrentCard(Card card) {
        this.currentCard = card;
    }

    Account getCurrentAccount() {
        return currentAccount;
    }

    void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }

    void setLastTransaction(Transaction transaction) {
        this.lastTransaction = transaction;
    }

    public Transaction getLastTransaction() {
        return lastTransaction;
    }

    public String getAtmId() {
        return atmId;
    }

    public String getStateName() {
        return currentState.getClass().getSimpleName();
    }
}
