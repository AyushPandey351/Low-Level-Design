package splitwise;

import java.util.ArrayList;
import java.util.List;

// A Group is just a named collection of members and the expenses that belong to
// them. Deliberately does NOT own the balance sheet itself - Balance is a separate
// class (SRP: Group manages membership/expenses, Balance manages the numbers). If
// Group tried to compute balances directly, every new expense would force Group to
// re-derive amounts owed, duplicating logic that Balance already owns.
//
// getBalances() is still declared here because from the outside world, "get this
// group's balances" is a natural question to ask a Group - but internally it just
// delegates to a Balance instance rather than computing anything itself.
public class Group {
    private final String id;
    private final String name;
    private final List<User> members;
    private final List<Expense> expenses;
    private final Balance balance;

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.members = new ArrayList<>();
        this.expenses = new ArrayList<>();
        this.balance = new Balance();
    }

    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    // Recording an expense here does two things: keep the expense in this group's
    // history, AND fold its splits into the running balance sheet. These two steps
    // must happen together - an expense that's "recorded" but never applied to
    // balances (or vice versa) would leave the group in an inconsistent state.
    public void addExpense(Expense expense) {
        expenses.add(expense);
        for (Split split : expense.getSplits()) {
            User debtor = split.getUser();
            User creditor = expense.getPaidBy();
            if (!debtor.equals(creditor)) {
                balance.updateBalance(debtor, creditor, split.getAmount());
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<User> getMembers() {
        return members;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public Balance getBalance() {
        return balance;
    }
}
