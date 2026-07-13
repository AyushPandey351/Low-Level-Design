package atm;

public enum Denomination {
    FIVE_HUNDRED(500),
    TWO_HUNDRED(200),
    HUNDRED(100);

    private final int value;

    Denomination(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
