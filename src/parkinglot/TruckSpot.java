package parkinglot;

public class TruckSpot extends ParkingSpot {
    public TruckSpot(int id) {
        super(id, SpotType.TRUCK);
    }

    @Override
    public boolean canPark(Vehicle vehicle) {
        return isAvailable() && vehicle.getType() == VehicleType.TRUCK;
    }
}
