package bookmyshow;

// Plain data-holder, same reasoning as every other User/Player/Customer in this
// series: searchMovie()/bookTicket() need MovieCatalog/SeatLockService/PaymentService,
// which User shouldn't hold references to - those live on the coordinating classes instead.
public class User {
    private final String id;
    private final String name;
    private final String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getUserId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name;
    }
}
