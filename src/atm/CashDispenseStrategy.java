package atm;

import java.util.Map;

// Strategy Pattern, same shape as every prior *Strategy in this series. CashDispenser
// depends on this interface, never a concrete LargestNotesFirstStrategy directly -
// tomorrow's policy change ("prefer smaller notes to conserve large-note stock") is
// a one-class addition, no changes to CashDispenser or ATM.
//
// Deliberately implementing two genuinely different, MEANINGFUL policies
// (largest-first, smallest-first) rather than literally following the notes'
// "RandomNotes" - dispensing a random note breakdown doesn't serve any real ATM
// policy goal, so it wouldn't demonstrate anything except that the interface
// technically permits it. Largest-vs-smallest-first actually produces visibly
// different note breakdowns for the same request, which is what proves the
// strategies are genuinely interchangeable.
public interface CashDispenseStrategy {
    // Returns denomination -> count needed to make up exactly `amount` from
    // `availableNotes`. Throws IllegalStateException if it's not possible (either
    // the amount isn't representable with these denominations, or there isn't
    // enough physical inventory).
    Map<Denomination, Integer> computeBreakdown(double amount, Map<Denomination, Integer> availableNotes);
}
