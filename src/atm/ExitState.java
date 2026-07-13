package atm;

// The session-ended terminal state. Its insertCard() delegates straight to a fresh
// IdleState's insertCard() rather than duplicating the card-validation logic here -
// ExitState IS effectively "ready for the next customer," it's just kept as its own
// named class (rather than transitioning straight to IdleState from
// CashDispensedState) to preserve the exact six-state sequence from your notes.
public class ExitState implements ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        new IdleState().insertCard(atm, card);
    }
}
