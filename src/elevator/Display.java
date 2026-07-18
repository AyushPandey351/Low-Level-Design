package elevator;

// Reacts to Elevator's own notifications instead of being polled - Elevator never
// calls display.show() directly; it calls notifyObservers(), and THIS class decides
// what "being notified" means (update local state, print it). That's the actual
// payoff of Observer here: Elevator's movement logic doesn't know a Display exists,
// only that some list of ElevatorObserver instances wants to hear about changes.
public class Display implements ElevatorObserver {
    private int currentFloor;
    private Direction direction = Direction.IDLE;

    @Override
    public void onFloorChanged(Elevator elevator, int newFloor) {
        this.currentFloor = newFloor;
        show(elevator);
    }

    @Override
    public void onStateChanged(Elevator elevator, ElevatorState newState) {
        show(elevator);
    }

    private void show(Elevator elevator) {
        System.out.println("[Display #" + elevator.getId() + "] Floor " + currentFloor
                + " | " + elevator.getDirection() + " | " + elevator.getState());
    }
}
