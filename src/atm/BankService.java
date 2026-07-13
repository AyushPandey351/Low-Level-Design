package atm;

// "ATM should not directly update accounts" - this is the gateway layer that
// enforces that boundary. Notice it's a thin layer: authenticate() checks card
// validity + PIN, and withdraw()/deposit() just forward to Account's own
// synchronized methods rather than re-implementing the money-safety logic here.
// This mirrors a pattern used throughout this series (PaymentRepository sits in
// front of Payment's own synchronized transitions; SeatLockService sits in front of
// ShowSeat's own synchronized transitions): the object that OWNS an invariant
// (Account owning "balance can never go negative, mutations must be atomic")
// enforces it itself; the service layer above is for routing, lookup, and
// authorization, not for re-implementing that invariant a second time.
//
// Singleton, same initialize()/getInstance() split used for ParkingLot/PaymentProcessor
// in earlier designs - a "the one banking backend this ATM talks to" service is
// naturally a single shared instance.
public class BankService {
    private static BankService instance;

    private BankService() {
    }

    public static synchronized BankService getInstance() {
        if (instance == null) {
            instance = new BankService();
        }
        return instance;
    }

    public Account authenticate(Card card, String pin) {
        if (!card.validate()) {
            throw new IllegalStateException("Card " + card.getCardNumber() + " is expired");
        }
        Account account = card.getAccount();
        if (!account.isPinCorrect(pin)) {
            throw new IllegalArgumentException("Incorrect PIN");
        }
        return account;
    }

    public void withdraw(Account account, double amount) {
        account.withdraw(amount);
    }

    public void deposit(Account account, double amount) {
        account.deposit(amount);
    }

    public double checkBalance(Account account) {
        return account.checkBalance();
    }

    public void changePin(Account account, String oldPin, String newPin) {
        account.changePin(oldPin, newPin);
    }
}
