package paymentgateway;

// The full lifecycle from your notes. Modeled as a plain enum here, with the actual
// transition RULES (which states can move to which) enforced inside Payment itself
// (see Payment.authorize()/capture()/fail()/refund()) rather than a full GoF State
// pattern (separate PaymentState interface + one class per state). Your notes flag
// State Pattern as "optional but impressive" - it's the right call for a system with
// many state-dependent BEHAVIORS (not just transition validity), but here the only
// thing that varies per state is "which transitions are legal," which a simple guard
// clause per method expresses just as clearly with far less machinery. If a future
// requirement demands different BEHAVIOR per state (e.g. "AUTHORIZED payments must
// send a reminder email, CAPTURED ones must not"), that's the point to introduce the
// full State pattern - not before that need exists.
public enum PaymentStatus {
    CREATED,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED
}
