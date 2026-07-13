package atm;

// Hardware stub - same simulated-boundary approach as GatewayConnector (Payment
// Gateway) and PaymentService (BookMyShow): the real implementation would talk to
// physical card-reading hardware; here it just represents that interaction.
public class CardReader {
    public Card readCard(Card card) {
        System.out.println("[CardReader] Card read: " + card.getCardNumber());
        return card;
    }

    public void ejectCard() {
        System.out.println("[CardReader] Card ejected");
    }
}
