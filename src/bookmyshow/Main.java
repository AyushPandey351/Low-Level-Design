package bookmyshow;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SeatLockService.initialize(Duration.ofSeconds(2)); // short timeout so the demo can actually observe expiry

        Movie movie = new Movie("M1", "Inception", Duration.ofMinutes(148), "English");

        List<Seat> seatLayout = Arrays.asList(
                new Seat("R1C1", SeatType.REGULAR, 1, 1),
                new Seat("R1C2", SeatType.REGULAR, 1, 2),
                new Seat("R1C3", SeatType.PREMIUM, 1, 3),
                new Seat("R2C1", SeatType.REGULAR, 2, 1),
                new Seat("R2C2", SeatType.PREMIUM, 2, 2),
                new Seat("R2C3", SeatType.RECLINER, 2, 3));
        Screen screen = new Screen("SCR1", seatLayout);
        Theatre theatre = new Theatre("T1", "PVR Cinemas", "MG Road");
        theatre.addScreen(screen);

        LocalDateTime weekdayShowTime = LocalDateTime.now().plusHours(2);
        Show weekdayShow = new Show("SHOW1", movie, screen, weekdayShowTime);

        MovieCatalog catalog = new MovieCatalog();
        catalog.addShow(weekdayShow);

        // --- Browse APIs ---
        System.out.println("== searchMovie(\"Inception\") ==");
        System.out.println(catalog.searchMovie("Inception"));
        System.out.println("== getShows(movie) ==");
        System.out.println(catalog.getShows(movie));
        System.out.println("Available seats on " + weekdayShow.getId() + ": " + weekdayShow.getAvailableSeats().size());

        // --- Concurrency test: Ayush and Rahul both click the SAME seat at the same instant ---
        System.out.println("\n== Concurrency: Ayush and Rahul both lock seat R2C3 AT THE SAME INSTANT ==");
        Seat contestedSeat = seatLayout.get(5); // R2C3
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> resultAyush = new AtomicReference<>();
        AtomicReference<String> resultRahul = new AtomicReference<>();

        Thread ayushThread = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                weekdayShow.lockSeats(List.of(contestedSeat), "U_AYUSH", SeatLockService.getInstance());
                resultAyush.set("LOCKED IT");
            } catch (IllegalStateException e) {
                resultAyush.set("FAILED: " + e.getMessage());
            }
        });
        Thread rahulThread = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                weekdayShow.lockSeats(List.of(contestedSeat), "U_RAHUL", SeatLockService.getInstance());
                resultRahul.set("LOCKED IT");
            } catch (IllegalStateException e) {
                resultRahul.set("FAILED: " + e.getMessage());
            }
        });
        ayushThread.start();
        rahulThread.start();
        startLatch.countDown();
        ayushThread.join();
        rahulThread.join();
        System.out.println("Ayush: " + resultAyush.get());
        System.out.println("Rahul: " + resultRahul.get());

        // --- Main happy path: Ayush books two REGULAR seats ---
        System.out.println("\n== Ayush books R1C1, R1C2 ==");
        User ayush = new User("U_AYUSH", "Ayush", "ayush@example.com");
        List<Seat> ayushSeats = Arrays.asList(seatLayout.get(0), seatLayout.get(1));
        weekdayShow.lockSeats(ayushSeats, ayush.getUserId(), SeatLockService.getInstance());
        System.out.println("Seats locked.");

        Map<SeatType, Double> baseRates = new HashMap<>();
        baseRates.put(SeatType.REGULAR, 200.0);
        baseRates.put(SeatType.PREMIUM, 350.0);
        baseRates.put(SeatType.RECLINER, 500.0);
        PricingStrategy normalPricing = new NormalPricingStrategy(baseRates);
        double fee = normalPricing.calculateFee(weekdayShow, ayushSeats);
        System.out.println("Fee for 2 REGULAR seats (weekday): " + fee);

        PaymentFactory paymentFactory = new PaymentFactory();
        PaymentService paymentService = new PaymentService();
        Payment payment = paymentFactory.createPayment(fee);
        paymentService.pay(payment, true);
        System.out.println("Payment " + payment.getPaymentId() + ": " + payment.getStatus());

        BookingFactory bookingFactory = new BookingFactory();
        Booking ayushBooking = bookingFactory.createBooking(
                ayush, weekdayShow, weekdayShow.toShowSeats(ayushSeats), fee);
        ayushBooking.confirm();
        System.out.println("Booking " + ayushBooking.getBookingId() + ": " + ayushBooking.getStatus());
        printSeatStatuses(weekdayShow, ayushSeats);

        // --- Weekend pricing comparison, on a genuinely different Show instance ---
        System.out.println("\n== Weekend pricing comparison ==");
        LocalDate nextSaturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        Show weekendShow = new Show("SHOW2", movie, screen, nextSaturday.atTime(19, 0));
        PricingStrategy weekendPricing = new WeekendPricingStrategy(normalPricing, 1.5);
        System.out.println("Weekday (" + weekdayShow.getStartTime().getDayOfWeek() + ") fee: "
                + weekendPricing.calculateFee(weekdayShow, ayushSeats));
        System.out.println("Saturday (" + weekendShow.getStartTime().getDayOfWeek() + ") fee: "
                + weekendPricing.calculateFee(weekendShow, ayushSeats));

        // --- Lock expiry demo: Neha locks a seat, then abandons the flow ---
        System.out.println("\n== Neha locks R2C1 and abandons checkout - lock should expire ==");
        Seat abandonedSeat = seatLayout.get(3); // R2C1
        weekdayShow.lockSeats(List.of(abandonedSeat), "U_NEHA", SeatLockService.getInstance());
        System.out.println("Status right after locking: "
                + weekdayShow.toShowSeats(List.of(abandonedSeat)).get(0).getStatus());
        Thread.sleep(2500); // wait past the 2-second lock timeout
        SeatLockService.getInstance().releaseExpiredLocks();
        System.out.println("Status after timeout + sweep: "
                + weekdayShow.toShowSeats(List.of(abandonedSeat)).get(0).getStatus());

        // --- Payment failure demo: Vikram locks a seat, payment declines ---
        System.out.println("\n== Vikram locks R2C2, payment FAILS ==");
        User vikram = new User("U_VIKRAM", "Vikram", "vikram@example.com");
        Seat vikramSeat = seatLayout.get(4); // R2C2
        weekdayShow.lockSeats(List.of(vikramSeat), vikram.getUserId(), SeatLockService.getInstance());
        double vikramFee = normalPricing.calculateFee(weekdayShow, List.of(vikramSeat));
        Payment vikramPayment = paymentFactory.createPayment(vikramFee);
        paymentService.pay(vikramPayment, false);
        System.out.println("Payment " + vikramPayment.getPaymentId() + ": " + vikramPayment.getStatus());
        Booking vikramBooking = bookingFactory.createBooking(
                vikram, weekdayShow, weekdayShow.toShowSeats(List.of(vikramSeat)), vikramFee);
        vikramBooking.fail();
        System.out.println("Booking status: " + vikramBooking.getStatus()
                + ", seat status: " + weekdayShow.toShowSeats(List.of(vikramSeat)).get(0).getStatus());

        // --- Cancellation demo: Ayush cancels his confirmed booking ---
        System.out.println("\n== Ayush cancels his confirmed booking ==");
        ayushBooking.cancel();
        System.out.println("Booking status: " + ayushBooking.getStatus());
        printSeatStatuses(weekdayShow, ayushSeats);
    }

    private static void printSeatStatuses(Show show, List<Seat> seats) {
        for (ShowSeat showSeat : show.toShowSeats(seats)) {
            System.out.println("  " + showSeat.getSeat().getId() + ": " + showSeat.getStatus());
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
