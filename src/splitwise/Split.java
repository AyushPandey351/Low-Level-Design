package splitwise;

// A single fact: "this user's share of some expense is this amount".
// Immutable on purpose - a Split is a calculated result, not something that should
// be edited after the fact. If it needs to change, recalculate via SplitStrategy
// instead of mutating it, so the expense and the split never drift out of sync.
//
// Could have used Map<User, Double> inside Expense instead of List<Split>, but a named
// type gives us room to grow (e.g. adding a "percentage" field for audit trail later)
// without restructuring Expense, and reads more clearly as "a list of shares".
public class Split {
    private final User user;
    private final double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return user.getName() + " owes " + amount;
    }
}
