package ringiot;

public interface EventDeduplicationService {
    // Atomic check-and-mark (the Redis SETNX the notes describe) - returns true
    // only the first time a given eventId is seen (or the first time after its TTL
    // expired). A naive separate "exists?" then "set" pair would have the same
    // check-then-act race flagged in the Ring Notification Service design; SETNX
    // is specifically an atomic primitive for exactly this reason.
    boolean trySetIfAbsent(String eventId);
}
