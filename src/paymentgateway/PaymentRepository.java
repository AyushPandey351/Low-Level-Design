package paymentgateway;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// Two maps: paymentsById (the actual store) and idempotencyKeyToPaymentId (the
// dedup index). Both are ConcurrentHashMap - this system is explicitly required to
// "handle concurrent scheduling/payment requests," so a plain HashMap would be a real
// bug under load (concurrent modification, or worse, silently lost writes), not just
// a theoretical concern.
//
// reserveIdempotencyKey() is THE fix for the double-click race from your Step 8 notes.
// A naive fix would be:
//     if (map.containsKey(key)) return map.get(key);   // check
//     map.put(key, newPaymentId);                       // then act
// That's a classic check-then-act race: two threads can both pass the `containsKey`
// check before either calls `put`, and both proceed to create a real Payment - exactly
// the "double-clicked Pay Now" bug you're trying to prevent. putIfAbsent() is a SINGLE
// atomic operation on ConcurrentHashMap: only one concurrent caller can ever be the
// one who successfully sets a fresh key, and it tells you which outcome happened via
// its return value - no window exists for two threads to both "win".
public class PaymentRepository {
    private final Map<String, Payment> paymentsById = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyKeyToPaymentId = new ConcurrentHashMap<>();

    public void save(Payment payment) {
        paymentsById.put(payment.getPaymentId(), payment);
    }

    public Payment find(String paymentId) {
        return paymentsById.get(paymentId);
    }

    public void update(Payment payment) {
        paymentsById.put(payment.getPaymentId(), payment);
    }

    // Returns null if `newPaymentId` just became the owner of this idempotency key
    // (caller should proceed to actually create/charge a new Payment). Returns the
    // ALREADY-existing paymentId if someone else got there first (caller must return
    // that existing payment instead of charging again).
    public String reserveIdempotencyKey(String idempotencyKey, String newPaymentId) {
        return idempotencyKeyToPaymentId.putIfAbsent(idempotencyKey, newPaymentId);
    }
}
