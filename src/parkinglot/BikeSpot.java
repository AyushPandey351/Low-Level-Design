package parkinglot;

public class BikeSpot extends ParkingSpot {
    public BikeSpot(int id) {
        super(id, SpotType.BIKE);
    }

    @Override
    public boolean canPark(Vehicle vehicle) {
        return isAvailable() && vehicle.getType() == VehicleType.BIKE;
    }
}
