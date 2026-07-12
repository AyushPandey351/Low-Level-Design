package splitwise.strategy;

import splitwise.Split;
import splitwise.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 900 / 3 people = 300 each - the easy case. But 1000 / 3 = 333.33...33, and if we
// naively round each share to 333.33, three shares sum to 999.99, not 1000 - the
// classic "missing paisa" bug. We fix it by giving the leftover (the remainder after
// integer-paisa division) to the first few participants, one paisa each, so the
// shares always sum to EXACTLY totalAmount. Balances must always reconcile to the
// paisa; a rounding leak here would quietly corrupt the whole balance sheet over time.
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculate(double totalAmount, List<User> participants, Map<User, Double> shareValues) {
        int n = participants.size();
        if (n == 0) {
            throw new IllegalArgumentException("Cannot split among zero participants");
        }

        // Work in paisa (integer) to avoid floating point drift, then convert back.
        long totalPaisa = Math.round(totalAmount * 100);
        long basePaisa = totalPaisa / n;
        long remainderPaisa = totalPaisa % n;

        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long sharePaisa = basePaisa + (i < remainderPaisa ? 1 : 0);
            splits.add(new Split(participants.get(i), sharePaisa / 100.0));
        }
        return splits;
    }
}
