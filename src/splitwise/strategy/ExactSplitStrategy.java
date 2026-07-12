package splitwise.strategy;

import splitwise.Split;
import splitwise.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// e.g. total = 1000, shareValues = {A:100, B:300, C:600}. Here shareValues holds the
// EXACT amount each user owes, straight from the caller (e.g. a UI where the payer
// manually types each person's share).
//
// We validate the amounts sum to the total. Without this check, a typo (100+300+500
// instead of 600) would silently create or destroy money in the balance sheet -
// a bug that's very hard to trace back once it's baked into everyone's balances.
public class ExactSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    @Override
    public List<Split> calculate(double totalAmount, List<User> participants, Map<User, Double> shareValues) {
        List<Split> splits = new ArrayList<>();
        double sum = 0;
        for (User user : participants) {
            Double amount = shareValues.get(user);
            if (amount == null) {
                throw new IllegalArgumentException("Missing exact amount for user " + user);
            }
            splits.add(new Split(user, amount));
            sum += amount;
        }

        if (Math.abs(sum - totalAmount) > EPSILON) {
            throw new IllegalArgumentException(
                    "Exact splits (" + sum + ") do not add up to total amount (" + totalAmount + ")");
        }
        return splits;
    }
}
