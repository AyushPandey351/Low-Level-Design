package parkinglot;

// Factory Pattern: centralizes the "given a type, give me the right concrete class"
// decision in ONE place, instead of scattering `if (type == CAR) new Car(...) else
// if...` across EntranceGate, Main, tests, etc. If a caller wants a vehicle, it says
// WHAT it wants (the type), not HOW to construct it - and if a new vehicle type is
// added, only this switch needs a new branch, not every call site that creates vehicles.
public class VehicleFactory {
    public static Vehicle createVehicle(VehicleType type, String vehicleNumber) {
        switch (type) {
            case CAR:
                return new Car(vehicleNumber);
            case BIKE:
                return new Bike(vehicleNumber);
            case TRUCK:
                return new Truck(vehicleNumber);
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
