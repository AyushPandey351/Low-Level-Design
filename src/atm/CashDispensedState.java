package atm;

// Named after the withdrawal flow specifically (matching your notes' six-state
// list), but used here as the generic "transaction completed, awaiting eject" state
// for ALL four transaction types - not just withdrawals. Introducing a second,
// parallel "completed" state purely to rename this for deposit/balance/PIN-change
// would add a class without adding any different behavior, since ejectCard() here
// does the same thing regardless of which transaction type just finished.
public class CashDispensedState implements ATMState {
    @Override
    public void ejectCard(ATM atm) {
        atm.getCardReader().ejectCard();
        atm.getScreen().displayMessage("Thank you for banking with us.");
        atm.setCurrentCard(null);
        atm.setCurrentAccount(null);
        atm.setState(new ExitState());
    }
}
