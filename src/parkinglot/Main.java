package parkinglot;

import parkinglot.strategy.FlatRatePricingStrategy;
import parkinglot.strategy.HourlyPricingStrategy;
import parkinglot.strategy.PricingStrategy;
import parkinglot.strategy.WeekendPricingStrategy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<VehicleType, Double> hourlyRates = new HashMap<>();
        hourlyRates.put(VehicleType.BIKE, 10.0);
        hourlyRates.put(VehicleType.CAR, 20.0);
        hourlyRates.put(VehicleType.TRUCK, 30.0);
        PricingStrategy pricingStrategy = new HourlyPricingStrategy(hourlyRates);

        ParkingLot lot = ParkingLot.initialize("Downtown Lot", "123 Main St", pricingStrategy);

        Floor floor1 = new Floor(1);
        floor1.addSpot(ParkingSpotFactory.createSpot(SpotType.BIKE, 1));
        floor1.addSpot(ParkingSpotFactory.createSpot(SpotType.CAR, 2));
        floor1.addSpot(ParkingSpotFactory.createSpot(SpotType.TRUCK, 3));
        lot.addFloor(floor1);

        Floor floor2 = new Floor(2);
        floor2.addSpot(ParkingSpotFactory.createSpot(SpotType.CAR, 4));
        lot.addFloor(floor2);

        EntranceGate entranceGate = new EntranceGate(lot);
        ExitGate exitGate = new ExitGate(lot);

        // --- Car enters 3 hours ago (so exiting "now" produces a real, non-zero fee) ---
        System.out.println("== Car KA01AB1234 enters ==");
        Vehicle car = VehicleFactory.createVehicle(VehicleType.CAR, "KA01AB1234");
        Ticket carTicket = entranceGate.issueTicket(car, LocalDateTime.now().minusHours(3));
        System.out.println("Ticket issued: " + carTicket.getTicketId()
                + ", spot " + carTicket.getSpot().getId() + " (floor 1)");

        // --- Bike and Truck also enter, to show different spot types being matched ---
        System.out.println("\n== Bike KA01XY9999 enters ==");
        Vehicle bike = VehicleFactory.createVehicle(VehicleType.BIKE, "KA01XY9999");
        Ticket bikeTicket = entranceGate.issueTicket(bike, LocalDateTime.now().minusMinutes(30));
        System.out.println("Ticket issued: " + bikeTicket.getTicketId() + ", spot " + bikeTicket.getSpot().getId());

        System.out.println("\n== Truck KA01TR0007 enters ==");
        Vehicle truck = VehicleFactory.createVehicle(VehicleType.TRUCK, "KA01TR0007");
        Ticket truckTicket = entranceGate.issueTicket(truck, LocalDateTime.now().minusMinutes(65));
        System.out.println("Ticket issued: " + truckTicket.getTicketId() + ", spot " + truckTicket.getSpot().getId());

        // --- A second car enters and must go to floor 2, since floor 1's only CarSpot is taken ---
        System.out.println("\n== Second Car KA01ZZ4321 enters ==");
        Vehicle car2 = VehicleFactory.createVehicle(VehicleType.CAR, "KA01ZZ4321");
        Ticket car2Ticket = entranceGate.issueTicket(car2, LocalDateTime.now());
        System.out.println("Ticket issued: " + car2Ticket.getTicketId()
                + ", spot " + car2Ticket.getSpot().getId() + " (floor 2 - floor 1's car spot was full)");

        // --- No truck spots left anywhere: this must fail loudly, not silently ---
        System.out.println("\n== Third vehicle (Truck) tries to enter, but no truck spots remain ==");
        try {
            Vehicle truck2 = VehicleFactory.createVehicle(VehicleType.TRUCK, "KA01TR0008");
            entranceGate.issueTicket(truck2, LocalDateTime.now());
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        // --- First car exits: fee is calculated, paid, and the spot is released ---
        System.out.println("\n== Car KA01AB1234 exits ==");
        LocalDateTime exitTime = LocalDateTime.now();
        double fee = exitGate.calculateAmount(carTicket, exitTime);
        System.out.printf("Parked for %d minutes -> fee = %.2f%n", carTicket.getParkingDuration(exitTime).toMinutes(), fee);
        Payment payment = exitGate.processExit(carTicket, exitTime);
        System.out.println("Payment status: " + payment.getStatus());
        System.out.println("Spot " + carTicket.getSpot().getId() + " available again: " + carTicket.getSpot().isAvailable());

        // --- Now that a CarSpot on floor 1 is free again, a new car should be able to grab it ---
        System.out.println("\n== Third Car KA01QQ5555 enters (should reuse the just-released spot) ==");
        Vehicle car3 = VehicleFactory.createVehicle(VehicleType.CAR, "KA01QQ5555");
        Ticket car3Ticket = entranceGate.issueTicket(car3, LocalDateTime.now());
        System.out.println("Ticket issued: " + car3Ticket.getTicketId() + ", spot " + car3Ticket.getSpot().getId());

        // --- Same Ticket, different PricingStrategy: proves the strategies are interchangeable ---
        System.out.println("\n== Comparing pricing strategies on the same completed car ticket ==");
        double hourlyFee = pricingStrategy.calculateFee(car2Ticket, exitTime.plusHours(2));
        PricingStrategy weekendStrategy = new WeekendPricingStrategy(pricingStrategy, 1.5);
        double weekendFee = weekendStrategy.calculateFee(car2Ticket, exitTime.plusHours(2));
        PricingStrategy flatStrategy = new FlatRatePricingStrategy(50.0);
        double flatFee = flatStrategy.calculateFee(car2Ticket, exitTime.plusHours(2));
        System.out.printf("Hourly: %.2f | Weekend (1.5x if entry was Sat/Sun): %.2f | Flat: %.2f%n",
                hourlyFee, weekendFee, flatFee);
    }
}
