package atm;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// The ReentrantLock here is "ATM Cash Lock" from Step 8: two withdrawals racing for
// the LAST available note of some denomination is a different bug from the account
// balance race - even if each account's balance is debited correctly (thanks to
// Account's own synchronized methods), TWO physical dispense operations could still
// both compute a breakdown that uses the same last note, if note allocation itself
// isn't serialized. Locking around "compute breakdown, then actually decrement
// inventory" as one atomic block is what prevents that - the second concurrent
// caller can't even start allocating until the first has fully committed its
// inventory decrement.
public class CashDispenser {
    private final Map<Denomination, Integer> notes;
    private final ReentrantLock lock = new ReentrantLock();

    public CashDispenser(Map<Denomination, Integer> notes) {
        this.notes = notes;
    }

    public boolean hasEnoughCash(double amount, CashDispenseStrategy strategy) {
        lock.lock();
        try {
            strategy.computeBreakdown(amount, notes);
            return true;
        } catch (IllegalStateException e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Map<Denomination, Integer> dispenseCash(double amount, CashDispenseStrategy strategy) {
        lock.lock();
        try {
            Map<Denomination, Integer> breakdown = strategy.computeBreakdown(amount, notes);
            for (Map.Entry<Denomination, Integer> entry : breakdown.entrySet()) {
                notes.merge(entry.getKey(), -entry.getValue(), Integer::sum);
            }
            return breakdown;
        } finally {
            lock.unlock();
        }
    }

    public Map<Denomination, Integer> getNotes() {
        return notes;
    }
}
