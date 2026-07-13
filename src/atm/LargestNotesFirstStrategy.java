package atm;

import java.util.EnumMap;
import java.util.Map;

// Greedy from the highest denomination down - minimizes the total note count, which
// is the typical real-world ATM default (fewer notes = faster dispensing, less
// mechanical wear).
public class LargestNotesFirstStrategy implements CashDispenseStrategy {

    private static final Denomination[] DESCENDING = {
            Denomination.FIVE_HUNDRED, Denomination.TWO_HUNDRED, Denomination.HUNDRED
    };

    @Override
    public Map<Denomination, Integer> computeBreakdown(double amount, Map<Denomination, Integer> availableNotes) {
        return greedyBreakdown(amount, availableNotes, DESCENDING);
    }

    static Map<Denomination, Integer> greedyBreakdown(double amount, Map<Denomination, Integer> availableNotes,
                                                        Denomination[] order) {
        int remaining = (int) Math.round(amount);
        Map<Denomination, Integer> breakdown = new EnumMap<>(Denomination.class);

        for (Denomination denomination : order) {
            int available = availableNotes.getOrDefault(denomination, 0);
            int needed = Math.min(available, remaining / denomination.getValue());
            if (needed > 0) {
                breakdown.put(denomination, needed);
                remaining -= needed * denomination.getValue();
            }
        }

        if (remaining != 0) {
            throw new IllegalStateException(
                    "Cannot dispense " + amount + " with available notes " + availableNotes);
        }
        return breakdown;
    }
}
