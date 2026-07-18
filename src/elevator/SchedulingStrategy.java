package elevator;

import java.util.List;

// Strategy Pattern, textbook per your notes - ElevatorController holds this
// INTERFACE, never a concrete NearestElevatorStrategy directly. LeastBusy, VIP, or
// energy-saving policies later are one-class additions, zero changes to
// ElevatorController.
public interface SchedulingStrategy {
    Elevator selectElevator(List<Elevator> elevators, HallRequest request);
}
