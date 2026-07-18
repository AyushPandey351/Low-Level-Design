package elevator;

// Observer Pattern, per your notes' "Display observes elevator movement." Elevator
// publishes state changes to whoever's subscribed, without knowing or caring who
// that is - Display is the one implementation built here, but a monitoring
// dashboard or an analytics logger could subscribe the exact same way later with
// zero changes to Elevator.
public interface ElevatorObserver {
    void onFloorChanged(Elevator elevator, int newFloor);

    void onStateChanged(Elevator elevator, ElevatorState newState);
}
