package splitwise;

import splitwise.strategy.EqualSplitStrategy;
import splitwise.strategy.ExactSplitStrategy;
import splitwise.strategy.PercentageSplitStrategy;

import java.util.HashMap;
import java.util.Map;

// Runnable walkthrough. This is where the design earns its keep - everything above
// is just classes sitting there until something actually drives them end to end.
public class Main {
    public static void main(String[] args) {
        ExpenseManager manager = new ExpenseManager();

        User ayush = new User("U1", "Ayush", "ayush@example.com");
        User rahul = new User("U2", "Rahul", "rahul@example.com");
        User rohan = new User("U3", "Rohan", "rohan@example.com");

        Group trip = manager.createGroup("Goa Trip");
        trip.addMember(ayush);
        trip.addMember(rahul);
        trip.addMember(rohan);

        // --- Step 6 walkthrough: restaurant bill, 900, Ayush paid, equal split ---
        System.out.println("== Expense 1: Restaurant bill, 900, paid by Ayush, equal split ==");
        manager.createExpense(trip, ayush, 900.0, new EqualSplitStrategy(), null);
        manager.printBalances(trip);
        // Expect: Rahul owes Ayush 300.00, Rohan owes Ayush 300.00

        // --- Exact split: cab fare, 1000, paid by Rahul, uneven exact shares ---
        System.out.println("\n== Expense 2: Cab fare, 1000, paid by Rahul, exact split (100/300/600) ==");
        Map<User, Double> exactShares = new HashMap<>();
        exactShares.put(ayush, 100.0);
        exactShares.put(rahul, 300.0);
        exactShares.put(rohan, 600.0);
        manager.createExpense(trip, rahul, 1000.0, new ExactSplitStrategy(), exactShares);
        manager.printBalances(trip);

        // --- Percentage split: hotel bill, 3000, paid by Rohan, 40/30/30 ---
        System.out.println("\n== Expense 3: Hotel bill, 3000, paid by Rohan, percentage split (40/30/30) ==");
        Map<User, Double> percentageShares = new HashMap<>();
        percentageShares.put(ayush, 40.0);
        percentageShares.put(rahul, 30.0);
        percentageShares.put(rohan, 30.0);
        manager.createExpense(trip, rohan, 3000.0, new PercentageSplitStrategy(), percentageShares);
        manager.printBalances(trip);

        // --- Settle: Rahul pays Ayush 300 in cash ---
        System.out.println("\n== Rahul settles 300 with Ayush in cash ==");
        manager.settle(trip, rahul, ayush, 300.0);
        manager.printBalances(trip);

        System.out.println("\n== Net balances ==");
        for (User user : trip.getMembers()) {
            System.out.printf("%s: %.2f%n", user.getName(), manager.showBalance(trip, user));
        }
    }
}
