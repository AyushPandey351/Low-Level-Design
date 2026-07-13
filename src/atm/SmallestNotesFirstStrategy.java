package atm;

import java.util.Map;

// Same greedy algorithm as LargestNotesFirstStrategy, just walking the denomination
// order smallest-to-largest instead - reuses the shared helper to prove the only
// real difference between these two policies is which order they consider notes in,
// not the underlying allocation logic.
public class SmallestNotesFirstStrategy implements CashDispenseStrategy {

    private static final Denomination[] ASCENDING = {
            Denomination.HUNDRED, Denomination.TWO_HUNDRED, Denomination.FIVE_HUNDRED
    };

    @Override
    public Map<Denomination, Integer> computeBreakdown(double amount, Map<Denomination, Integer> availableNotes) {
        return LargestNotesFirstStrategy.greedyBreakdown(amount, availableNotes, ASCENDING);
    }
}
