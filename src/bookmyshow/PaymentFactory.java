package bookmyshow;

import java.util.concurrent.atomic.AtomicInteger;

// Lightweight construction factory - same flavor as MeetingFactory in the Meeting
// Scheduler design: no branching logic, just centralized id generation so every call
// site that needs a fresh Payment record doesn't repeat it.
public class PaymentFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public Payment createPayment(double amount) {
        return new Payment("PAY" + counter.incrementAndGet(), amount);
    }
}
