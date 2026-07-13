package atm;

// If authenticate() throws (wrong PIN), setState() is never reached - the ATM simply
// stays in CardInsertedState, which naturally allows the customer to retry
// authenticate() without any extra retry-counting code needed here. A real ATM would
// add a retry-limit wrapper around this, but that's a policy decision layered on top
// of this state, not a change to the state machine itself.
public class CardInsertedState implements ATMState {
    @Override
    public void authenticate(ATM atm, String pin) {
        Account account = atm.getBankService().authenticate(atm.getCurrentCard(), pin);
        atm.setCurrentAccount(account);
        atm.getScreen().displayMessage("Authenticated. Please select a transaction.");
        atm.setState(new AuthenticatedState());
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.getCardReader().ejectCard();
        atm.setCurrentCard(null);
        atm.getScreen().displayMessage("Card ejected.");
        atm.setState(new ExitState());
    }
}
