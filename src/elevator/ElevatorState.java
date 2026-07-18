package elevator;

// MAINTENANCE is added beyond the notes' original three values, directly implementing
// the Amazon follow-up question ("how would you support maintenance mode?") rather
// than just discussing it. Per that follow-up's own answer, a MAINTENANCE elevator
// is simply excluded from scheduling (see NearestElevatorStrategy/LeastBusyElevatorStrategy)
// - no change needed to ElevatorController's logic at all, which is the whole point
// of having asked the question that way.
public enum ElevatorState {
    IDLE,
    MOVING,
    DOOR_OPEN,
    MAINTENANCE
}
