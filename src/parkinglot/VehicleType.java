package parkinglot;

// A plain enum, not a class hierarchy, because "type" here is just a label used for
// matching vehicles to compatible spots (see ParkingSpot.canPark). There's no distinct
// BEHAVIOR per type living on the enum itself - that behavior lives on the Vehicle
// subclasses instead. If BIKE/CAR/TRUCK needed different logic (not just a different
// label), an enum would be the wrong tool - you'd want the class hierarchy to carry it.
public enum VehicleType {
    BIKE,
    CAR,
    TRUCK
}
