package splitwise;

import java.util.HashMap;
import java.util.Map;

// The ledger. Instead of recomputing "who owes whom" from the full expense history
// every time someone asks (O(expenses) per query, and awkward to reason about), we
// maintain a running total that's updated incrementally as each expense/settlement
// happens. This is a classic space-for-time tradeoff: O(1) updates and reads, at the
// cost of having to keep the ledger consistent as we go (which addExpense/settle do).
//
// balances.get(A).get(B) = amount B owes A. Only ONE direction is stored per pair -
// if Rahul later owes Ayush *and* Ayush owes Rahul, updateBalance nets them against
// each other so the map never holds two contradictory entries for the same pair.
public class Balance {
    private final Map<User, Map<User, Double>> balances = new HashMap<>();

    private static final double EPSILON = 0.01;

    // Records that `debtor` owes `creditor` an additional `amount`.
    // Nets against any existing debt in the opposite direction first, so the ledger
    // always stores at most one non-zero direction per pair of users.
    public void updateBalance(User debtor, User creditor, double amount) {
        double existingReverse = getDirectAmount(creditor, debtor);
        if (existingReverse > 0) {
            // creditor already owed debtor some amount - reduce that first.
            double net = existingReverse - amount;
            if (net > EPSILON) {
                setDirectAmount(creditor, debtor, net);
                setDirectAmount(debtor, creditor, 0);
            } else {
                setDirectAmount(creditor, debtor, 0);
                setDirectAmount(debtor, creditor, -net);
            }
        } else {
            double existing = getDirectAmount(debtor, creditor);
            setDirectAmount(debtor, creditor, existing + amount);
        }
    }

    // Positive result: `other` owes `user`. Negative: `user` owes `other`.
    public double getBalanceBetween(User user, User other) {
        return getDirectAmount(other, user) - getDirectAmount(user, other);
    }

    // Net balance of `user` against everyone: positive means `user` is owed money overall.
    public double getNetBalance(User user) {
        double net = 0;
        for (User other : allCounterparties(user)) {
            net += getBalanceBetween(user, other);
        }
        return net;
    }

    // `payer` hands `amount` to `payee` in cash, reducing what payer owed payee.
    // NOTE the argument order passed to updateBalance is flipped (payee, payer), not
    // (payer, payee): settling cash is the OPPOSITE of incurring a new debt. If payer
    // owes payee 300 and pays 300, we need that 300 to CANCEL the existing debt, not
    // add another 300 on top of it. Recording it as "payee now owes payer 300" lets
    // updateBalance's existing netting logic cancel the original debt correctly.
    public void settle(User payer, User payee, double amount) {
        updateBalance(payee, payer, amount);
    }

    private double getDirectAmount(User debtor, User creditor) {
        return balances.getOrDefault(debtor, new HashMap<>()).getOrDefault(creditor, 0.0);
    }

    private void setDirectAmount(User debtor, User creditor, double amount) {
        if (amount <= EPSILON) {
            if (balances.containsKey(debtor)) {
                balances.get(debtor).remove(creditor);
            }
            return;
        }
        balances.computeIfAbsent(debtor, k -> new HashMap<>()).put(creditor, amount);
    }

    private java.util.Set<User> allCounterparties(User user) {
        java.util.Set<User> counterparties = new java.util.HashSet<>();
        counterparties.addAll(balances.getOrDefault(user, new HashMap<>()).keySet());
        for (Map.Entry<User, Map<User, Double>> entry : balances.entrySet()) {
            if (entry.getValue().containsKey(user)) {
                counterparties.add(entry.getKey());
            }
        }
        return counterparties;
    }

    public Map<User, Map<User, Double>> getRawBalances() {
        return balances;
    }
}
