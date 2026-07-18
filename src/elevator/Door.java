package elevator;

// Kept separate from Elevator, per your notes - "could be inside Elevator, but
// keeping separate is cleaner." Concretely, why: Door has its OWN lifecycle rules
// (guarded open/close transitions) and would, in a real system, own additional
// door-specific concerns like obstruction sensors and auto-close timers - none of
// which have anything to do with WHICH FLOOR the elevator is on or WHERE it's
// going. Bundling that into Elevator would mix two different responsibilities
// (motion vs. door safety) into one class; SRP says keep them apart.
//
// Guarded, synchronized transitions - same discipline as every stateful class in
// this series (Payment, ShowSeat, Meeting): open() only valid from CLOSED, close()
// only valid from OPEN.
public class Door {
    private DoorState state = DoorState.CLOSED;

    public synchronized void open() {
        if (state == DoorState.OPEN) {
            throw new IllegalStateException("Door is already open");
        }
        state = DoorState.OPEN;
    }

    public synchronized void close() {
        if (state == DoorState.CLOSED) {
            throw new IllegalStateException("Door is already closed");
        }
        state = DoorState.CLOSED;
    }

    public synchronized DoorState getState() {
        return state;
    }
}
