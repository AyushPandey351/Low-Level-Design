package elevator;

import java.util.List;

// Goes beyond pure distance - one of the Amazon follow-ups your notes flag
// ("elevator already moving in the same direction") is built in directly: an
// elevator moving AWAY from or past the requested floor is heavily penalized versus
// one that's IDLE or already heading toward it, even if the moving-away elevator is
// numerically closer right now. Pure nearest-by-distance alone would happily assign
// a request to an elevator that's about to move further away, which is obviously
// worse than a slightly-farther elevator already headed the right way.
//
// MAINTENANCE elevators are skipped entirely - directly implementing the other
// Amazon follow-up ("how would you support maintenance mode") without
// ElevatorController needing to know MAINTENANCE exists at all.
public class NearestElevatorStrategy implements SchedulingStrategy {
    private static final int WRONG_DIRECTION_PENALTY = 1000;

    @Override
    public Elevator selectElevator(List<Elevator> elevators, HallRequest request) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.MAINTENANCE) {
                continue;
            }
            int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
            int score = distance;
            if (elevator.getDirection() != Direction.IDLE && !isMovingToward(elevator, request)) {
                score += WRONG_DIRECTION_PENALTY;
            }
            if (score < bestScore) {
                bestScore = score;
                best = elevator;
            }
        }

        if (best == null) {
            throw new IllegalStateException("No elevator available to serve this request");
        }
        return best;
    }

    private boolean isMovingToward(Elevator elevator, HallRequest request) {
        if (elevator.getDirection() == Direction.UP) {
            return request.getSourceFloor() >= elevator.getCurrentFloor();
        }
        if (elevator.getDirection() == Direction.DOWN) {
            return request.getSourceFloor() <= elevator.getCurrentFloor();
        }
        return true; // IDLE can go either way
    }
}
