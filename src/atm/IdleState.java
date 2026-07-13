package atm;

// The only valid action from Idle is inserting a card - everything else (trying to
// authenticate, withdraw, etc. with no card in the machine) correctly falls through
// to ATMState's default reject.
public class IdleState implements ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        if (!card.validate()) {
            throw new IllegalStateException("Card " + card.getCardNumber() + " is expired");
        }
        atm.getCardReader().readCard(card);
        atm.setCurrentCard(card);
        atm.getScreen().displayMessage("Card inserted. Please enter your PIN.");
        atm.setState(new CardInsertedState());
    }
}
