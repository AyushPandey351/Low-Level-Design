package elevator;

import java.util.List;

// Singleton, per your notes - "only one controller." Same initialize()/getInstance()
// split used throughout this series, since it needs a configured elevator list and
// strategy at creation time.
//
// requestElevator() is `synchronized` - this is the literal answer to the Amazon
// follow-up "how would you make it thread-safe... keep the controller's scheduling
// logic atomic to avoid assigning the same request twice." Selection
// (strategy.selectElevator) and assignment (elevator.addRequest) must happen as ONE
// atomic step: if two hall requests were selected concurrently without this lock,
// both could read the SAME elevator's current state as "best," both assign to it,
// and a genuinely better second elevator would sit unused - not incorrect exactly,
// but a real scheduling-quality bug that only shows up under concurrent load.
// Synchronizing the whole method serializes scheduling DECISIONS (fast: distance
// math over 5 elevators) without serializing the elevators' own movement (each
// Elevator's step()/addRequest() has its own independent lock) - so this doesn't
// become a throughput bottleneck for the actual moving parts.
public class ElevatorController {
    private static ElevatorController instance;

    private final List<Elevator> elevators;
    private volatile SchedulingStrategy strategy;

    private ElevatorController(List<Elevator> elevators, SchedulingStrategy strategy) {
        this.elevators = elevators;
        this.strategy = strategy;
    }

    public static synchronized ElevatorController initialize(List<Elevator> elevators, SchedulingStrategy strategy) {
        if (instance != null) {
            throw new IllegalStateException("ElevatorController is already initialized");
        }
        instance = new ElevatorController(elevators, strategy);
        return instance;
    }

    public static synchronized ElevatorController getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ElevatorController has not been initialized");
        }
        return instance;
    }

    public synchronized Elevator requestElevator(HallRequest request) {
        Elevator elevator = strategy.selectElevator(elevators, request);
        elevator.addRequest(request.getSourceFloor());
        return elevator;
    }

    public void submitDestination(int elevatorId, CarRequest request) {
        Elevator elevator = findElevator(elevatorId);
        elevator.addRequest(request.getDestinationFloor());
    }

    // Drives one simulation step for every elevator - a real deployment would call
    // this on a fixed-rate scheduler thread; this demo's Main drives it directly in
    // a loop for deterministic, observable output.
    public void tick() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
    }

    // Changing the algorithm at runtime - `strategy` is `volatile` so this takes
    // effect for every concurrent requestElevator() caller immediately, same
    // visibility reasoning as RateLimiter.updateConfig() in the Rate Limiter design.
    public void setStrategy(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setMaintenanceMode(int elevatorId, boolean underMaintenance) {
        findElevator(elevatorId).setMaintenanceMode(underMaintenance);
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    private Elevator findElevator(int elevatorId) {
        for (Elevator elevator : elevators) {
            if (elevator.getId() == elevatorId) {
                return elevator;
            }
        }
        throw new IllegalArgumentException("No such elevator: " + elevatorId);
    }
}
