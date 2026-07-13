package atm;

import java.time.LocalDate;

public class Card {
    private final String cardNumber;
    private final LocalDate expiry;
    private final Account account;

    public Card(String cardNumber, LocalDate expiry, Account account) {
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.account = account;
    }

    public boolean validate() {
        return !expiry.isBefore(LocalDate.now());
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public LocalDate getExpiry() {
        return expiry;
    }

    public Account getAccount() {
        return account;
    }
}
