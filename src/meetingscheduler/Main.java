package meetingscheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        AvailabilityService availabilityService = new AvailabilityService();
        MeetingFactory meetingFactory = new MeetingFactory();
        CalendarRepository calendarRepository = new CalendarRepository();
        NotificationService notificationService = new NotificationService();
        MeetingScheduler scheduler = new MeetingScheduler(
                availabilityService, meetingFactory, calendarRepository, notificationService);

        User ayush = new User("U1", "Ayush", "ayush@example.com");
        User rahul = new User("U2", "Rahul", "rahul@example.com");
        User charlie = new User("U3", "Charlie", "charlie@example.com");
        scheduler.registerUser(ayush);
        scheduler.registerUser(rahul);
        scheduler.registerUser(charlie);

        LocalDateTime day = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);

        // --- Ayush schedules a meeting with Rahul and Charlie ---
        System.out.println("== Ayush schedules 'Design Review' 10:00-11:00 with Rahul and Charlie ==");
        TimeSlot slot1 = new TimeSlot(day.plusHours(1), day.plusHours(2));
        Meeting meeting1 = scheduler.scheduleMeeting("Design Review", ayush, Arrays.asList(rahul, charlie), slot1);
        System.out.println("Meeting created: " + meeting1.getMeetingId() + ", status=" + meeting1.getStatus());

        System.out.println("\n== Rahul accepts, Charlie declines ==");
        scheduler.acceptInvite(meeting1.getMeetingId(), rahul.getUserId());
        scheduler.declineInvite(meeting1.getMeetingId(), charlie.getUserId());
        for (Participant p : meeting1.getParticipants()) {
            System.out.println(p.getUser().getName() + ": " + p.getStatus());
        }

        // --- A second, overlapping meeting for Charlie must be rejected ---
        System.out.println("\n== Rahul tries to book Charlie for an OVERLAPPING slot (10:30-11:30) ==");
        TimeSlot overlapping = new TimeSlot(day.plusHours(1).plusMinutes(30), day.plusHours(2).plusMinutes(30));
        try {
            scheduler.scheduleMeeting("Sync", rahul, Arrays.asList(charlie), overlapping);
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        // --- Find the next common free slot for all three within a search window ---
        System.out.println("\n== Finding earliest common slot for Ayush, Rahul, Charlie (30 min, within 09:00-18:00) ==");
        TimeSlot searchWindow = new TimeSlot(day, day.withHour(18));
        TimeSlot earliest = scheduler.findCommonSlot(
                ayush, Arrays.asList(rahul, charlie), Duration.ofMinutes(30), searchWindow, new EarliestSlotStrategy());
        System.out.println("Earliest common slot: " + earliest);

        // --- Preferred slot strategy: try a free preferred slot first ---
        System.out.println("\n== Preferred-slot search: 14:00-14:30 (free) ==");
        TimeSlot preferredFree = new TimeSlot(day.plusHours(5), day.plusHours(5).plusMinutes(30));
        SchedulingStrategy preferredStrategy = new PreferredSlotStrategy(preferredFree, new EarliestSlotStrategy());
        TimeSlot result1 = scheduler.findCommonSlot(
                ayush, Arrays.asList(rahul, charlie), Duration.ofMinutes(30), searchWindow, preferredStrategy);
        System.out.println("Result (should be exactly the preferred slot): " + result1);

        System.out.println("\n== Preferred-slot search: 10:00-11:00 (already booked - should fall back) ==");
        SchedulingStrategy preferredTaken = new PreferredSlotStrategy(slot1, new EarliestSlotStrategy());
        TimeSlot result2 = scheduler.findCommonSlot(
                ayush, Arrays.asList(rahul, charlie), Duration.ofMinutes(30), searchWindow, preferredTaken);
        System.out.println("Result (should NOT be 10:00-11:00, falls back to earliest free): " + result2);

        // --- Reschedule the original meeting ---
        System.out.println("\n== Rescheduling 'Design Review' to 15:00-16:00 ==");
        TimeSlot newSlot = new TimeSlot(day.plusHours(6), day.plusHours(7));
        scheduler.rescheduleMeeting(meeting1.getMeetingId(), newSlot);
        System.out.println("New slot: " + meeting1.getSlot());

        // --- Cancel it ---
        System.out.println("\n== Cancelling 'Design Review' ==");
        scheduler.cancelMeeting(meeting1.getMeetingId());
        System.out.println("Status: " + meeting1.getStatus());
        System.out.println("Charlie's calendar meeting count: " + charlie.viewCalendar().size());

        // --- Real concurrency test: two threads race to book overlapping slots for Charlie ---
        System.out.println("\n== Concurrency test: Ayush and Rahul both try to book Charlie for 10:00-11:00 AT THE SAME INSTANT ==");
        TimeSlot raceSlot = new TimeSlot(day.plusHours(1), day.plusHours(2));
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> resultA = new AtomicReference<>();
        AtomicReference<String> resultB = new AtomicReference<>();

        Thread threadA = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                scheduler.scheduleMeeting("Ayush's meeting", ayush, Arrays.asList(charlie), raceSlot);
                resultA.set("SUCCEEDED");
            } catch (IllegalStateException e) {
                resultA.set("REJECTED: " + e.getMessage());
            }
        });
        Thread threadB = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                scheduler.scheduleMeeting("Rahul's meeting", rahul, Arrays.asList(charlie), raceSlot);
                resultB.set("SUCCEEDED");
            } catch (IllegalStateException e) {
                resultB.set("REJECTED: " + e.getMessage());
            }
        });

        threadA.start();
        threadB.start();
        startLatch.countDown(); // release both threads at (as close to) the same instant
        threadA.join();
        threadB.join();

        System.out.println("Thread A (Ayush): " + resultA.get());
        System.out.println("Thread B (Rahul): " + resultB.get());
        System.out.println("Charlie's final calendar meeting count: " + charlie.viewCalendar().size()
                + " (must be exactly 1 - only one of the two racing requests may win)");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
