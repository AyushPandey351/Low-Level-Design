package parkinglot;

// Abstract base, not an interface, because every vehicle genuinely shares the SAME
// state (number plate, type) and the SAME implementation of how to expose it - there's
// no varying behavior to justify an interface here. Car/Bike/Truck exist as separate
// classes purely so the type system can distinguish them (useful later if a subtype
// ever needs its own field, e.g. Truck.axleCount) - today they're identical besides
// the type passed to super(). This is also what makes LSP concrete: ParkingSpot.canPark()
// and everything else in this design accepts a plain `Vehicle`, and any subtype
// (Car, Bike, Truck) can be substituted there with no surprises, because none of them
// override behavior in a way that would violate the base class's contract.
public abstract class Vehicle {
    private final String vehicleNumber;
    private final VehicleType type;

    protected Vehicle(String vehicleNumber, VehicleType type) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public VehicleType getType() {
        return type;
    }
}
