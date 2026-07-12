package splitwise;

import splitwise.strategy.SplitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The coordinator / facade. This is the ONE class that knows about everything else -
// Group, Expense, Balance, SplitStrategy - and it's the entry point client code (or
// Main, in our demo) actually talks to. Nobody outside this class should be doing
// `new Expense(...)` directly, because ExpenseManager is what guarantees an expense
// is always both recorded AND folded into the right group's balance sheet in the
// same step (see createExpense below) - skipping that guarantee is how balance sheets
// drift out of sync with expense history.
//
// Notice this class has almost no interesting logic of its own - it delegates
// everything (id generation aside) to Group/Expense/Balance. That's intentional:
// a coordinator's job is to orchestrate calls in the right order, not to duplicate
// the logic those classes already own.
public class ExpenseManager {
    private final List<Group> groups = new ArrayList<>();
    private int groupCounter = 0;
    private int expenseCounter = 0;

    public Group createGroup(String name) {
        groupCounter++;
        Group group = new Group("G" + groupCounter, name);
        groups.add(group);
        return group;
    }

    // Splits the bill among the group's own members by default. shareValues is only
    // meaningful for Exact/Percentage strategies - pass null (or an empty map) for Equal.
    public Expense createExpense(Group group, User paidBy, double amount,
                                  SplitStrategy strategy, Map<User, Double> shareValues) {
        expenseCounter++;
        String id = "E" + expenseCounter;
        // participants = the group's members. In a fuller app you might let the caller
        // pick a subset (e.g. "only these 3 of the 5 members were at dinner"), but for
        // this design we keep it simple: everyone in the group shares every expense.
        Expense expense = new Expense(id, amount, paidBy, strategy, group.getMembers(), shareValues);
        group.addExpense(expense); // records history AND updates balances, atomically from the caller's view
        return expense;
    }

    // Positive = user is owed money overall in this group; negative = user owes money overall.
    public double showBalance(Group group, User user) {
        return group.getBalance().getNetBalance(user);
    }

    // Prints every non-zero pairwise balance in the group - "who owes whom, how much".
    public void printBalances(Group group) {
        Map<User, Map<User, Double>> raw = group.getBalance().getRawBalances();
        boolean any = false;
        for (Map.Entry<User, Map<User, Double>> entry : raw.entrySet()) {
            User debtor = entry.getKey();
            for (Map.Entry<User, Double> owed : entry.getValue().entrySet()) {
                User creditor = owed.getKey();
                double amount = owed.getValue();
                if (amount > 0) {
                    System.out.printf("%s owes %s %.2f%n", debtor.getName(), creditor.getName(), amount);
                    any = true;
                }
            }
        }
        if (!any) {
            System.out.println("All settled up!");
        }
    }

    public void settle(Group group, User payer, User payee, double amount) {
        group.getBalance().settle(payer, payee, amount);
    }
}
