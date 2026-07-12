package parkinglot;

public class CarSpot extends ParkingSpot {
    public CarSpot(int id) {
        super(id, SpotType.CAR);
    }

    @Override
    public boolean canPark(Vehicle vehicle) {
        return isAvailable() && vehicle.getType() == VehicleType.CAR;
    }
}
