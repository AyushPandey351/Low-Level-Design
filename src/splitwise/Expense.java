package splitwise;

import splitwise.strategy.SplitStrategy;

import java.util.List;
import java.util.Map;

// Represents one bill: who paid, how much, and how it's divided. Expense depends on
// SplitStrategy (the interface), never on a concrete strategy class - that's the
// Dependency Inversion Principle from your notes. Expense's only job is to own the
// data and delegate "how do we divide this" to the strategy; it doesn't know or care
// whether that's equal/exact/percentage math.
public class Expense {
    private final String id;
    private final double amount;
    private final User paidBy;
    private final SplitStrategy strategy;
    private List<Split> splits;

    public Expense(String id, double amount, User paidBy, SplitStrategy strategy,
                    List<User> participants, Map<User, Double> shareValues) {
        this.id = id;
        this.amount = amount;
        this.paidBy = paidBy;
        this.strategy = strategy;
        // Splits are computed once, at construction time, not lazily on every getSplits()
        // call. An Expense is meant to represent a fact that already happened (a bill that
        // was already split) - recalculating on every read would let the same expense
        // silently produce different splits if participants/shareValues were mutable inputs.
        calculateSplits(participants, shareValues);
    }

    private void calculateSplits(List<User> participants, Map<User, Double> shareValues) {
        this.splits = strategy.calculate(amount, participants, shareValues);
    }

    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public List<Split> getSplits() {
        return splits;
    }
}
