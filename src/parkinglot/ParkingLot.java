package parkinglot;

import parkinglot.strategy.PricingStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Singleton, per your notes: only one parking lot should exist in this application
// (there's exactly one physical lot being modeled, so a second in-memory instance
// would just be a bug - two independent sets of floors/spots claiming to represent
// the same physical building).
//
// Split into initialize(...) + getInstance() instead of the textbook no-arg
// getInstance() that lazily builds itself. Reason: a real ParkingLot needs
// configuration (name, address, which PricingStrategy to charge) at creation time -
// there's no sensible default to lazily construct from nothing. So construction
// happens once, explicitly, via initialize(); every other caller just retrieves the
// already-built instance via getInstance(). This is a very common real-world variant
// of Singleton that textbook examples (which assume a no-arg constructor) skip over.
//
// getInstance() is `synchronized` as cheap insurance against two threads racing to
// read `instance` while initialize() is still running - not because this demo is
// multithreaded, but because "only one instance must ever exist" is exactly the kind
// of invariant that quietly breaks under concurrency if you don't guard it.
public class ParkingLot {
    private static ParkingLot instance;

    private final String name;
    private final String address;
    private final PricingStrategy pricingStrategy;
    private final List<Floor> floors = new ArrayList<>();
    private int ticketCounter = 0;

    private ParkingLot(String name, String address, PricingStrategy pricingStrategy) {
        this.name = name;
        this.address = address;
        this.pricingStrategy = pricingStrategy;
    }

    public static synchronized ParkingLot initialize(String name, String address, PricingStrategy pricingStrategy) {
        if (instance != null) {
            throw new IllegalStateException("ParkingLot is already initialized");
        }
        instance = new ParkingLot(name, address, pricingStrategy);
        return instance;
    }

    public static synchronized ParkingLot getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ParkingLot has not been initialized - call initialize() first");
        }
        return instance;
    }

    public void addFloor(Floor floor) {
        floors.add(floor);
    }

    // Searches floor by floor for the first spot that fits. ParkingLot doesn't know
    // HOW a floor searches its own spots (that's Floor.findAvailableSpot's job) -
    // it only decides which floors to ask, and in what order.
    public ParkingSpot findSpot(Vehicle vehicle) {
        for (Floor floor : floors) {
            ParkingSpot spot = floor.findAvailableSpot(vehicle);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        ParkingSpot spot = findSpot(vehicle);
        if (spot == null) {
            throw new IllegalStateException("No available spot for vehicle " + vehicle.getVehicleNumber());
        }
        spot.park(vehicle);
        ticketCounter++;
        return new Ticket("T" + ticketCounter, vehicle, spot, entryTime);
    }

    public void unparkVehicle(Ticket ticket) {
        ticket.getSpot().removeVehicle();
    }

    public PricingStrategy getPricingStrategy() {
        return pricingStrategy;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public List<Floor> getFloors() {
        return floors;
    }
}
