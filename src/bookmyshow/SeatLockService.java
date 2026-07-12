package bookmyshow;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// "Interviewers love this class" - and the reason is that ShowSeat's own guarded,
// synchronized lock() already prevents two users from BOTH successfully locking the
// same seat (see ShowSeat's comment). What ShowSeat CANNOT do is answer "who holds
// this lock" or "has this lock expired" - that's bookkeeping this class owns via its
// Map<ShowSeat, LockRecord>, exactly as your notes describe.
//
// Singleton via initialize()/getInstance() - same split as ParkingLot/PaymentProcessor
// in earlier designs, because this needs a configured lock timeout at creation time.
//
// lockSeats() is ALL-OR-NOTHING across the requested seats: a booking needs ALL
// selected seats together, so if seat 3 of 4 fails (already taken by someone else),
// the first 2 that WERE successfully locked in this same call must be rolled back -
// otherwise a failed multi-seat booking attempt would leave orphaned locks on seats
// the user never actually gets to book, silently starving other users of seats that
// look unavailable but aren't really being purchased.
public class SeatLockService {
    private static SeatLockService instance;

    private final Duration lockTimeout;
    private final Map<ShowSeat, LockRecord> locks = new ConcurrentHashMap<>();

    private SeatLockService(Duration lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public static synchronized SeatLockService initialize(Duration lockTimeout) {
        if (instance != null) {
            throw new IllegalStateException("SeatLockService is already initialized");
        }
        instance = new SeatLockService(lockTimeout);
        return instance;
    }

    public static synchronized SeatLockService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SeatLockService has not been initialized");
        }
        return instance;
    }

    public void lockSeats(List<ShowSeat> showSeats, String userId) {
        List<ShowSeat> lockedSoFar = new ArrayList<>();
        try {
            for (ShowSeat showSeat : showSeats) {
                expireIfStale(showSeat);
                try {
                    showSeat.lock();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException("Seat " + showSeat.getSeat().getId() + " is not available", e);
                }
                locks.put(showSeat, new LockRecord(userId, Instant.now()));
                lockedSoFar.add(showSeat);
            }
        } catch (RuntimeException e) {
            for (ShowSeat rollback : lockedSoFar) {
                locks.remove(rollback);
                rollback.release();
            }
            throw e;
        }
    }

    // Explicit release by whoever currently holds the lock (e.g. payment failed, or
    // the user backed out) - verifies OWNERSHIP first, so a second user can't release
    // a lock that isn't theirs just by guessing/reusing a ShowSeat reference.
    public void unlock(ShowSeat showSeat, String userId) {
        LockRecord record = locks.get(showSeat);
        if (record == null || !record.userId.equals(userId)) {
            throw new IllegalStateException(
                    "User " + userId + " does not hold the lock for seat " + showSeat.getSeat().getId());
        }
        locks.remove(showSeat);
        showSeat.release();
    }

    // Called once a seat is actually BOOKED (payment succeeded) - stops tracking it
    // for expiry entirely, so a slow sweep long after purchase can never mistakenly
    // release a seat someone has already paid for.
    public void markBooked(ShowSeat showSeat) {
        locks.remove(showSeat);
    }

    // A periodic sweep (a real system would run this on a background scheduler) that
    // releases any lock older than the configured timeout. Also defensively skips (and
    // just forgets) any tracked seat whose status is no longer LOCKED - it may have
    // already been booked or released through some other path.
    public void releaseExpiredLocks() {
        for (Map.Entry<ShowSeat, LockRecord> entry : locks.entrySet()) {
            ShowSeat showSeat = entry.getKey();
            if (showSeat.getStatus() != SeatStatus.LOCKED) {
                locks.remove(showSeat);
                continue;
            }
            if (isExpired(entry.getValue())) {
                locks.remove(showSeat);
                showSeat.release();
            }
        }
    }

    private void expireIfStale(ShowSeat showSeat) {
        LockRecord record = locks.get(showSeat);
        if (record != null && showSeat.getStatus() == SeatStatus.LOCKED && isExpired(record)) {
            locks.remove(showSeat);
            showSeat.release();
        }
    }

    private boolean isExpired(LockRecord record) {
        return Duration.between(record.lockedAt, Instant.now()).compareTo(lockTimeout) > 0;
    }

    private static class LockRecord {
        final String userId;
        final Instant lockedAt;

        LockRecord(String userId, Instant lockedAt) {
            this.userId = userId;
            this.lockedAt = lockedAt;
        }
    }
}
