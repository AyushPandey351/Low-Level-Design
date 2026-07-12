package splitwise.strategy;

import splitwise.Split;
import splitwise.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// e.g. shareValues = {A:40, B:30, C:30} (percentages). We validate they sum to 100
// for the same reason ExactSplitStrategy validates its sum against the total: catching
// a bad input (e.g. percentages summing to 90 because someone forgot a roommate) here,
// at calculation time, is far cheaper than debugging a mismatched balance sheet later.
public class PercentageSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    @Override
    public List<Split> calculate(double totalAmount, List<User> participants, Map<User, Double> shareValues) {
        List<Split> splits = new ArrayList<>();
        double percentageSum = 0;
        for (User user : participants) {
            Double percentage = shareValues.get(user);
            if (percentage == null) {
                throw new IllegalArgumentException("Missing percentage for user " + user);
            }
            percentageSum += percentage;
            double amount = totalAmount * percentage / 100.0;
            splits.add(new Split(user, amount));
        }

        if (Math.abs(percentageSum - 100.0) > EPSILON) {
            throw new IllegalArgumentException("Percentages must sum to 100, got " + percentageSum);
        }
        return splits;
    }
}
