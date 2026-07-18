package elevator;

// Holds a reference to ElevatorController and forwards button presses to it - the
// same "thin hardware gate" shape as EntranceGate/ExitGate in the Parking Lot design.
// Unlike User/Player/Customer elsewhere in this series (where the equivalent
// behavior was deliberately LEFT OFF the data class to avoid a circular dependency),
// pressUpButton()/pressDownButton() genuinely belong here: "generate a hall
// request" IS the physical button being pressed, and Floor doesn't need to know
// anything about scheduling or elevators beyond "hand this HallRequest to the
// controller" - the exact same shape as EntranceGate.issueTicket() delegating
// straight to ParkingLot.
public class Floor {
    private final int floorNumber;
    private final ElevatorController controller;

    public Floor(int floorNumber, ElevatorController controller) {
        this.floorNumber = floorNumber;
        this.controller = controller;
    }

    public void pressUpButton() {
        controller.requestElevator(new HallRequest(floorNumber, Direction.UP));
    }

    public void pressDownButton() {
        controller.requestElevator(new HallRequest(floorNumber, Direction.DOWN));
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}
