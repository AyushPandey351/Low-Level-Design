package atm;

import java.util.concurrent.atomic.AtomicInteger;

// Factory Pattern from your notes, but implemented as four typed methods rather than
// one method with an enum switch - worth calling out as a deliberate deviation from
// how the other factories in this series (VehicleFactory, ParkingSpotFactory,
// PaymentFactory) were built. Those all worked as a single switch because every
// variant needed the SAME constructor parameters (an id and one or two simple
// fields). Here the four transaction types need genuinely DIFFERENT collaborators -
// WithdrawTransaction needs a CashDispenser and strategy, PinChangeTransaction needs
// old/new PIN strings, BalanceInquiryTransaction needs neither. Forcing all of that
// into one method's signature would mean passing null for whatever's irrelevant to
// the requested type - worse than just having four small, honestly-typed methods.
// The Factory Pattern's actual goal (centralize construction + id generation, avoid
// scattering `new WithdrawTransaction(...)` at every call site) is fully achieved
// either way.
public class TransactionFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public WithdrawTransaction createWithdraw(Account account, double amount, BankService bankService,
                                               CashDispenser dispenser, CashDispenseStrategy strategy) {
        return new WithdrawTransaction(nextId(), account, amount, bankService, dispenser, strategy);
    }

    public DepositTransaction createDeposit(Account account, double amount, BankService bankService) {
        return new DepositTransaction(nextId(), account, amount, bankService);
    }

    public BalanceInquiryTransaction createBalanceInquiry(Account account, BankService bankService) {
        return new BalanceInquiryTransaction(nextId(), account, bankService);
    }

    public PinChangeTransaction createPinChange(Account account, BankService bankService,
                                                 String oldPin, String newPin) {
        return new PinChangeTransaction(nextId(), account, bankService, oldPin, newPin);
    }

    private String nextId() {
        return "TXN" + counter.incrementAndGet();
    }
}
