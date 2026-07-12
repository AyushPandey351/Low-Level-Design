package parkinglot;

import java.util.ArrayList;
import java.util.List;

// Owns the spots on one physical floor. findAvailableSpot() is a simple linear scan -
// fine for this design's scale, and it's the natural place for this logic to live
// (not in ParkingLot) because "search within one floor" is a floor-local concern.
// ParkingLot's job (next) is only to decide WHICH floor to ask, not how a floor
// searches its own spots - keeping that boundary is what SRP means in practice here.
public class Floor {
    private final int floorNumber;
    private final List<ParkingSpot> spots = new ArrayList<>();

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    public ParkingSpot findAvailableSpot(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canPark(vehicle)) {
                return spot;
            }
        }
        return null;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }
}
