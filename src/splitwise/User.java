package splitwise;

import java.util.Objects;

// Plain data-holder for a participant. Deliberately has NO createGroup()/addExpense()
// methods here, even though the original design listed them under User - that logic
// belongs to ExpenseManager/Group, otherwise User would need a back-reference to the
// manager and we'd get a circular dependency between User and ExpenseManager.
public class User {
    private final String id;
    private final String name;
    private final String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    // equals/hashCode are based on id only. This matters a lot: User is used as a key
    // in Map<User, Map<User, Double>> inside Balance. Without a correct equals/hashCode,
    // two User objects representing the same person (e.g. fetched separately) would be
    // treated as different map keys, silently splitting one person's balance in two.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Just the name, so printed balances read as "Rahul owes Ayush 300" instead of User@1a2b3c.
    @Override
    public String toString() {
        return name;
    }
}
