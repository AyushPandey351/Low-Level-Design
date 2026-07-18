package elevator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

// The core class. Implements a LOOK-style algorithm: keep moving in the CURRENT
// direction, serving every pending stop in that direction, and only reverse once
// that direction's queue is empty. This is exactly why upStops is ascending and
// downStops is descending (per your notes) - TreeSet.first() always hands back the
// NEAREST stop in the direction currently being traveled, letting move logic just
// repeatedly ask "what's next" without scanning or sorting anything itself.
//
// Every method that touches upStops/downStops/direction/state is `synchronized` -
// the same discipline as every stateful class in this series. This matters
// specifically because addRequest() can be called from a caller thread (a hall
// button being pressed, or ElevatorController assigning a request) at the same
// moment step() is being driven from a simulation-tick thread - without
// synchronization, a request could be added while move logic is mid-decision about
// which TreeSet to consult, corrupting the "what do I do next" decision.
public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private ElevatorState state = ElevatorState.IDLE;
    private final Door door = new Door();
    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>(Collections.reverseOrder());
    private final List<ElevatorObserver> observers = new ArrayList<>();

    public Elevator(int id, int startingFloor) {
        this.id = id;
        this.currentFloor = startingFloor;
    }

    // Immediately pushes the CURRENT floor/state to a newly added observer - without
    // this, a Display attached after the elevator already started somewhere other
    // than floor 0 would show a stale "floor 0" until the elevator's next move,
    // since observers only ever hear about CHANGES, never the state that already
    // existed before they subscribed. This is a common Observer-pattern gotcha:
    // a new subscriber needs an initial sync, not just a promise of future updates.
    public synchronized void addObserver(ElevatorObserver observer) {
        observers.add(observer);
        observer.onFloorChanged(this, currentFloor);
        observer.onStateChanged(this, state);
    }

    // Used for both hall requests (assigned by ElevatorController) and car requests
    // (selected by a passenger already inside) - both are ultimately "please stop at
    // this floor," which is all this method needs to know.
    public synchronized void addRequest(int floor) {
        if (floor > currentFloor) {
            upStops.add(floor);
        } else if (floor < currentFloor) {
            downStops.add(floor);
        }
        // floor == currentFloor: already here, nothing to queue.
    }

    // One discrete simulation tick - either finishes a door-open dwell, or advances
    // one floor toward the next stop. Kept to "one action per tick" deliberately,
    // so a demo driving this in a loop can observe each step individually.
    public synchronized void step() {
        if (state == ElevatorState.MAINTENANCE) {
            return; // excluded from operation entirely - see ElevatorState's comment
        }
        if (state == ElevatorState.DOOR_OPEN) {
            closeDoor();
            return;
        }
        moveOneFloor();
    }

    private void moveOneFloor() {
        if (direction == Direction.IDLE) {
            if (!upStops.isEmpty()) {
                direction = Direction.UP;
            } else if (!downStops.isEmpty()) {
                direction = Direction.DOWN;
            } else {
                return; // nothing to do
            }
        }

        if (direction == Direction.UP) {
            if (upStops.isEmpty()) {
                reverseOrIdle();
                return;
            }
            currentFloor++;
            setState(ElevatorState.MOVING);
            notifyFloorChanged();
            if (currentFloor == upStops.first()) {
                upStops.pollFirst();
                arriveAtStop();
            }
        } else if (direction == Direction.DOWN) {
            if (downStops.isEmpty()) {
                reverseOrIdle();
                return;
            }
            currentFloor--;
            setState(ElevatorState.MOVING);
            notifyFloorChanged();
            if (currentFloor == downStops.first()) {
                downStops.pollFirst();
                arriveAtStop();
            }
        }
    }

    // Current direction's queue just emptied - LOOK's reversal step: switch to the
    // other direction if it has pending stops, otherwise go IDLE.
    private void reverseOrIdle() {
        if (direction == Direction.UP && !downStops.isEmpty()) {
            direction = Direction.DOWN;
        } else if (direction == Direction.DOWN && !upStops.isEmpty()) {
            direction = Direction.UP;
        } else {
            direction = Direction.IDLE;
            setState(ElevatorState.IDLE);
        }
    }

    private void arriveAtStop() {
        openDoor();
        setState(ElevatorState.DOOR_OPEN);
    }

    private void openDoor() {
        door.open();
    }

    private void closeDoor() {
        door.close();
        if (upStops.isEmpty() && downStops.isEmpty()) {
            direction = Direction.IDLE;
            setState(ElevatorState.IDLE);
        } else {
            setState(ElevatorState.MOVING);
        }
    }

    // Only notifies on a GENUINE transition - moveOneFloor() calls setState(MOVING)
    // on every single tick while moving, even though the state was already MOVING.
    // Without this guard, observers would get a spurious "state changed" notification
    // every tick alongside the real floor-changed one, doubling up Display's output
    // for no actual state change at all.
    private void setState(ElevatorState newState) {
        if (this.state == newState) {
            return;
        }
        this.state = newState;
        notifyStateChanged();
    }

    private void notifyFloorChanged() {
        for (ElevatorObserver observer : observers) {
            observer.onFloorChanged(this, currentFloor);
        }
    }

    private void notifyStateChanged() {
        for (ElevatorObserver observer : observers) {
            observer.onStateChanged(this, state);
        }
    }

    public synchronized void setMaintenanceMode(boolean underMaintenance) {
        this.state = underMaintenance ? ElevatorState.MAINTENANCE : ElevatorState.IDLE;
    }

    public synchronized int getPendingStopCount() {
        return upStops.size() + downStops.size();
    }

    public int getId() {
        return id;
    }

    public synchronized int getCurrentFloor() {
        return currentFloor;
    }

    public synchronized Direction getDirection() {
        return direction;
    }

    public synchronized ElevatorState getState() {
        return state;
    }
}
