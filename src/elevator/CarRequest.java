package elevator;

// The INSIDE button press - "take me to floor 14." Kept as its own class rather than
// reusing HallRequest, even though both ultimately just carry a floor number, because
// they represent conceptually different events from different actors (a person
// outside the elevator vs. a passenger already inside it) - collapsing them would
// blur that distinction for no real benefit, since a HallRequest also carries a
// Direction that a CarRequest has no use for (a passenger already inside doesn't
// need to declare which way they want to go, only where).
public class CarRequest {
    private final int destinationFloor;

    public CarRequest(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }
}
