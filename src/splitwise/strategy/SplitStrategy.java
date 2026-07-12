package splitwise.strategy;

import splitwise.Split;
import splitwise.User;

import java.util.List;
import java.util.Map;

// Strategy Pattern: Expense doesn't know HOW splitting works, only THAT something
// implementing this interface can do it. This is Dependency Inversion in action -
// Expense depends on this abstraction, never on EqualSplitStrategy/ExactSplitStrategy
// directly. Tomorrow, adding "WeightedSplitStrategy" means writing one new class;
// zero existing classes change (Open/Closed Principle).
//
// shareValues is deliberately generic (Map<User, Double>) because each strategy
// interprets it differently:
//   - EqualSplitStrategy: ignores it entirely
//   - ExactSplitStrategy: user -> exact amount they owe
//   - PercentageSplitStrategy: user -> percentage they owe
// An alternative design would give each strategy its own calculate() signature, but
// then Expense couldn't hold a single "SplitStrategy strategy" field polymorphically -
// it would need to know which concrete type it has, defeating the whole point.
public interface SplitStrategy {
    List<Split> calculate(double totalAmount, List<User> participants, Map<User, Double> shareValues);
}
