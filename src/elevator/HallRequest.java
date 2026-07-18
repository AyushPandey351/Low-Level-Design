package elevator;

// One outside button press - "I'm on floor 5, I want to go UP." Immutable, same
// reasoning as every request/event value object in this series (Move, Payment's
// PaymentRequest, OrderPlacedEvent): it represents a fact that already happened
// (the button was pressed), not something that should mutate afterward.
public class HallRequest {
    private final int sourceFloor;
    private final Direction direction;

    public HallRequest(int sourceFloor, Direction direction) {
        this.sourceFloor = sourceFloor;
        this.direction = direction;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public Direction getDirection() {
        return direction;
    }
}
