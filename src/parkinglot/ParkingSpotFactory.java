package parkinglot;

// Same reasoning as VehicleFactory: whoever builds up a Floor's spots (Main, in our
// demo) just says "give me a CAR spot with id 5", without knowing CarSpot exists as
// a concrete class. Keeps spot construction in one place so adding EVSpot/VIPSpot
// later means one new branch here, not edits everywhere spots get created.
public class ParkingSpotFactory {
    public static ParkingSpot createSpot(SpotType type, int id) {
        switch (type) {
            case CAR:
                return new CarSpot(id);
            case BIKE:
                return new BikeSpot(id);
            case TRUCK:
                return new TruckSpot(id);
            default:
                throw new IllegalArgumentException("Unknown spot type: " + type);
        }
    }
}
