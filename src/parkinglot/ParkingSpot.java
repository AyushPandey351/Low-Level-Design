package parkinglot;

// Abstract class (not interface) for the same reason as Vehicle: every spot shares
// real state (id, occupied, the vehicle currently parked there) and most of its
// behavior (park/removeVehicle/isAvailable) is identical across subtypes - only
// canPark() actually differs per spot type, so that's the one method left abstract.
//
// canPark() is where the "which vehicle fits which spot" rule lives, and it's declared
// here so every ParkingSpot subtype is FORCED to define its own compatibility rule -
// there's no way to add a new spot type and forget this method, since the compiler
// won't let you extend ParkingSpot without implementing it.
public abstract class ParkingSpot {
    private final int id;
    private final SpotType type;
    private boolean occupied;
    private Vehicle vehicle;

    protected ParkingSpot(int id, SpotType type) {
        this.id = id;
        this.type = type;
        this.occupied = false;
    }

    public abstract boolean canPark(Vehicle vehicle);

    // Deliberately not just "!occupied" - a spot could be free but still the wrong
    // type for the given vehicle, and callers (Floor.findAvailableSpot) need both
    // checks. Keeping them as two separate methods (isAvailable + canPark) lets each
    // stay simple and lets Floor combine them however it needs to.
    public void park(Vehicle vehicle) {
        if (occupied) {
            throw new IllegalStateException("Spot " + id + " is already occupied");
        }
        this.vehicle = vehicle;
        this.occupied = true;
    }

    public void removeVehicle() {
        this.vehicle = null;
        this.occupied = false;
    }

    public boolean isAvailable() {
        return !occupied;
    }

    public int getId() {
        return id;
    }

    public SpotType getType() {
        return type;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
