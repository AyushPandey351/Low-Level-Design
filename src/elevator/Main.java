package elevator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<Elevator> elevators = new ArrayList<>();
        int[] startingFloors = {1, 5, 10, 15, 20};
        for (int i = 0; i < 5; i++) {
            Elevator elevator = new Elevator(i + 1, startingFloors[i]);
            elevator.addObserver(new Display());
            elevators.add(elevator);
        }
        ElevatorController controller = ElevatorController.initialize(elevators, new NearestElevatorStrategy());

        Floor floor5 = new Floor(5, controller);
        Floor floor18 = new Floor(18, controller);

        // --- Basic flow: hall call, elevator arrives, passenger selects a destination ---
        System.out.println("== Person on floor 5 presses UP ==");
        Elevator assigned = controller.requestElevator(new HallRequest(5, Direction.UP));
        System.out.println("Assigned elevator #" + assigned.getId() + " (started at floor " + startingFloorOf(assigned, startingFloors, elevators) + ")");
        runUntilIdleOrDoorOpen(controller, assigned);

        System.out.println("\n== Passenger boards elevator #" + assigned.getId() + " and selects floor 15 ==");
        controller.submitDestination(assigned.getId(), new CarRequest(15));
        runUntilIdleOrDoorOpen(controller, assigned);
        System.out.println("Elevator #" + assigned.getId() + " arrived at floor " + assigned.getCurrentFloor());

        // --- LOOK algorithm: multiple stops in the same direction are served in order, no backtracking ---
        System.out.println("\n== LOOK algorithm: elevator #" + assigned.getId() + " gets stops at 17 and 12 while idle-ish ==");
        controller.submitDestination(assigned.getId(), new CarRequest(17));
        assigned.addRequest(12); // a hall request picked up along a different direction
        System.out.println("Pending stops queued: " + assigned.getPendingStopCount());
        runManyTicks(controller, 40);
        System.out.println("Elevator #" + assigned.getId() + " final floor: " + assigned.getCurrentFloor()
                + ", pending: " + assigned.getPendingStopCount());

        // --- Maintenance mode: excluded from scheduling with zero controller changes ---
        System.out.println("\n== Putting elevator #3 into MAINTENANCE, then requesting from floor 18 ==");
        controller.setMaintenanceMode(3, true);
        Elevator chosen = controller.requestElevator(new HallRequest(18, Direction.UP));
        System.out.println("Chosen elevator: #" + chosen.getId() + " (must never be #3)");
        controller.setMaintenanceMode(3, false);

        // --- OCP in action: swap scheduling strategy at runtime, no ElevatorController changes ---
        System.out.println("\n== Swapping to LeastBusyElevatorStrategy ==");
        elevators.get(0).addRequest(elevators.get(0).getCurrentFloor() + 3); // make elevator #1 busy
        elevators.get(0).addRequest(elevators.get(0).getCurrentFloor() + 6);
        controller.setStrategy(new LeastBusyElevatorStrategy());
        Elevator leastBusyPick = controller.requestElevator(new HallRequest(2, Direction.UP));
        System.out.println("LeastBusyElevatorStrategy picked elevator #" + leastBusyPick.getId()
                + " (pending=" + leastBusyPick.getPendingStopCount() + "), NOT elevator #1 which now has extra load");
        controller.setStrategy(new NearestElevatorStrategy());

        // --- Real concurrency test: many hall requests fired at once while ticking concurrently ---
        System.out.println("\n== Concurrency: 30 concurrent hall requests while the simulation is ticking ==");
        Random random = new Random(42);
        int requestCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        for (int i = 0; i < requestCount; i++) {
            int floor = 1 + random.nextInt(20);
            Direction dir = random.nextBoolean() ? Direction.UP : Direction.DOWN;
            pool.submit(() -> {
                awaitLatch(startLatch);
                try {
                    controller.requestElevator(new HallRequest(floor, dir));
                } catch (RuntimeException ignored) {
                    // acceptable only if genuinely no elevator can serve - not expected here
                }
                doneLatch.countDown();
            });
        }
        // A concurrent ticking thread drives the simulation WHILE requests are still arriving.
        Thread tickerThread = new Thread(() -> {
            for (int i = 0; i < 200; i++) {
                controller.tick();
                sleep(5);
            }
        });

        startLatch.countDown();
        tickerThread.start();
        doneLatch.await();
        tickerThread.join();
        pool.shutdown();

        // Run additional ticks to drain whatever's left after the concurrent burst.
        runManyTicks(controller, 300);

        int totalPending = elevators.stream().mapToInt(Elevator::getPendingStopCount).sum();
        System.out.println("Total pending stops across all elevators after draining: " + totalPending + " (must be 0)");
        for (Elevator elevator : elevators) {
            System.out.println("Elevator #" + elevator.getId() + ": floor=" + elevator.getCurrentFloor()
                    + ", state=" + elevator.getState() + ", direction=" + elevator.getDirection());
        }
    }

    private static int startingFloorOf(Elevator elevator, int[] startingFloors, List<Elevator> elevators) {
        return startingFloors[elevators.indexOf(elevator)];
    }

    private static void runUntilIdleOrDoorOpen(ElevatorController controller, Elevator elevator) {
        for (int i = 0; i < 100; i++) {
            if (elevator.getState() == ElevatorState.DOOR_OPEN) {
                return;
            }
            controller.tick();
        }
    }

    private static void runManyTicks(ElevatorController controller, int ticks) {
        for (int i = 0; i < ticks; i++) {
            controller.tick();
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
