package elevator;

import java.util.List;

// The "tomorrow" strategy from your notes, built now to prove the OCP point
// concretely: swapping ElevatorController from NearestElevatorStrategy to this one
// is a one-line change at the point of construction - nothing about
// ElevatorController, Elevator, or HallRequest changes. Picks whichever
// (non-maintenance) elevator currently has the fewest pending stops queued, ignoring
// distance entirely - a genuinely different policy, not just a distance tweak, which
// is what makes this a meaningful second Strategy implementation rather than a
// near-duplicate of NearestElevatorStrategy.
public class LeastBusyElevatorStrategy implements SchedulingStrategy {
    @Override
    public Elevator selectElevator(List<Elevator> elevators, HallRequest request) {
        Elevator best = null;
        int bestPendingCount = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.MAINTENANCE) {
                continue;
            }
            int pending = elevator.getPendingStopCount();
            if (pending < bestPendingCount) {
                bestPendingCount = pending;
                best = elevator;
            }
        }

        if (best == null) {
            throw new IllegalStateException("No elevator available to serve this request");
        }
        return best;
    }
}
