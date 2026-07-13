package atm;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        BankService bankService = BankService.getInstance();
        Account ayushAccount = new Account("ACC1", 1000.0, "1234");
        Card ayushCard = new Card("CARD1", LocalDate.now().plusYears(2), ayushAccount);

        Map<Denomination, Integer> notes = new EnumMap<>(Denomination.class);
        notes.put(Denomination.FIVE_HUNDRED, 10);
        notes.put(Denomination.TWO_HUNDRED, 10);
        notes.put(Denomination.HUNDRED, 10);
        CashDispenser dispenser = new CashDispenser(notes);

        ATM atm = new ATM("ATM1", new CardReader(), new Screen(), new Keypad(), dispenser,
                bankService, new ReceiptPrinter(), new TransactionFactory(), new LargestNotesFirstStrategy());

        // --- Rejecting an operation in the wrong state ---
        System.out.println("== Trying to withdraw before inserting a card ==");
        try {
            atm.withdraw(100);
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        // --- Happy path: insert card, wrong PIN then correct PIN, check balance, withdraw, eject ---
        System.out.println("\n== Insert card, wrong PIN, then correct PIN ==");
        atm.insertCard(ayushCard);
        try {
            atm.authenticate("0000");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected as expected: " + e.getMessage() + " (state stays: " + atm.getStateName() + ")");
        }
        atm.authenticate("1234");
        System.out.println("State: " + atm.getStateName());

        System.out.println("\n== Check balance ==");
        atm.checkBalance();
        atm.ejectCard();
        System.out.println("State after eject: " + atm.getStateName());

        System.out.println("\n== New session: withdraw 800 ==");
        atm.insertCard(ayushCard);
        atm.authenticate("1234");
        atm.withdraw(800);
        System.out.println("Cash left in dispenser: " + dispenser.getNotes());
        atm.ejectCard();

        // --- Insufficient funds: state returns to AuthenticatedState, not stuck ---
        System.out.println("\n== New session: try to withdraw more than the remaining balance ==");
        atm.insertCard(ayushCard);
        atm.authenticate("1234");
        try {
            atm.withdraw(10_000);
        } catch (InsufficientFundsException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }
        System.out.println("State after failure (should still allow another attempt): " + atm.getStateName());
        atm.checkBalance();
        atm.ejectCard();

        // --- Deposit, then check balance, then change PIN - each its own session, since
        // this state machine is intentionally linear (one transaction per card session,
        // matching the notes' six-state sequence): CashDispensedState only allows
        // ejectCard(), not a second transaction in the same session. ---
        System.out.println("\n== New session: deposit 300 ==");
        atm.insertCard(ayushCard);
        atm.authenticate("1234");
        atm.deposit(300);
        atm.ejectCard();

        System.out.println("\n== New session: check balance reflects the deposit ==");
        atm.insertCard(ayushCard);
        atm.authenticate("1234");
        atm.checkBalance();
        atm.ejectCard();

        System.out.println("\n== New session: change PIN ==");
        atm.insertCard(ayushCard);
        atm.authenticate("1234");
        atm.changePin("1234", "5678");
        atm.ejectCard();
        System.out.println("\n== Confirm new PIN works, old PIN doesn't ==");
        atm.insertCard(ayushCard);
        try {
            atm.authenticate("1234");
        } catch (IllegalArgumentException e) {
            System.out.println("Old PIN rejected as expected: " + e.getMessage());
        }
        atm.authenticate("5678");
        System.out.println("New PIN accepted, state: " + atm.getStateName());
        atm.ejectCard();

        // --- Cash-dispensing failure triggers rollback: account must not lose money it never got ---
        System.out.println("\n== Withdrawal that passes the balance check but the dispenser can't make exactly ==");
        Map<Denomination, Integer> scarceNotes = new EnumMap<>(Denomination.class);
        scarceNotes.put(Denomination.FIVE_HUNDRED, 0);
        scarceNotes.put(Denomination.TWO_HUNDRED, 1);
        scarceNotes.put(Denomination.HUNDRED, 1);
        CashDispenser scarceDispenser = new CashDispenser(scarceNotes); // can only make 300 total, in specific combos
        ATM scarceAtm = new ATM("ATM2", new CardReader(), new Screen(), new Keypad(), scarceDispenser,
                bankService, new ReceiptPrinter(), new TransactionFactory(), new LargestNotesFirstStrategy());
        double balanceBefore = bankService.checkBalance(ayushAccount);
        scarceAtm.insertCard(ayushCard);
        scarceAtm.authenticate("5678");
        try {
            scarceAtm.withdraw(500); // account has plenty of balance, but dispenser has no combination for exactly 500
        } catch (IllegalStateException e) {
            System.out.println("Dispense failed as expected: " + e.getMessage());
        }
        double balanceAfter = bankService.checkBalance(ayushAccount);
        System.out.println("Balance before: " + balanceBefore + ", after failed withdrawal + rollback: " + balanceAfter
                + " (must be equal - the debit was rolled back)");
        scarceAtm.ejectCard();

        // --- Concurrency test 1: Account Lock - two ATMs withdraw from the SAME account at once ---
        // Uses a FRESH account (balance exactly 1000, matching your notes' scenario precisely) rather
        // than reusing ayushAccount's already-mutated balance - 800 + 500 = 1300 must genuinely exceed
        // the starting balance for this to be a meaningful test. Without synchronization, both threads
        // could read the stale balance=1000 before either writes, letting both succeed and driving the
        // balance to -300; with Account's synchronized methods, exactly one must win.
        Account raceAccount = new Account("ACC2", 1000.0, "1111");
        Card raceCard = new Card("CARD2", LocalDate.now().plusYears(2), raceAccount);
        System.out.println("\n== Concurrency: two ATMs withdraw 800 and 500 from the same account (balance=" +
                bankService.checkBalance(raceAccount) + ") simultaneously ==");
        ATM atmA = new ATM("ATM_A", new CardReader(), new Screen(), new Keypad(), new CashDispenser(freshNotes()),
                bankService, new ReceiptPrinter(), new TransactionFactory(), new LargestNotesFirstStrategy());
        ATM atmB = new ATM("ATM_B", new CardReader(), new Screen(), new Keypad(), new CashDispenser(freshNotes()),
                bankService, new ReceiptPrinter(), new TransactionFactory(), new LargestNotesFirstStrategy());
        atmA.insertCard(raceCard);
        atmA.authenticate("1111");
        atmB.insertCard(raceCard);
        atmB.authenticate("1111");

        double raceBalance = bankService.checkBalance(raceAccount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> resultA = new AtomicReference<>();
        AtomicReference<String> resultB = new AtomicReference<>();
        Thread threadA = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                atmA.withdraw(800);
                resultA.set("SUCCEEDED");
            } catch (RuntimeException e) {
                resultA.set("FAILED: " + e.getMessage());
            }
        });
        Thread threadB = new Thread(() -> {
            awaitLatch(startLatch);
            try {
                atmB.withdraw(500);
                resultB.set("SUCCEEDED");
            } catch (RuntimeException e) {
                resultB.set("FAILED: " + e.getMessage());
            }
        });
        threadA.start();
        threadB.start();
        startLatch.countDown();
        threadA.join();
        threadB.join();
        System.out.println("ATM A (withdraw 800): " + resultA.get());
        System.out.println("ATM B (withdraw 500): " + resultB.get());
        System.out.println("Final balance: " + bankService.checkBalance(raceAccount)
                + " (must be exactly " + raceBalance + " minus whichever ONE withdrawal succeeded, and never negative)");

        // --- Concurrency test 2: Cash Dispenser Lock - two threads race for the LAST note ---
        System.out.println("\n== Concurrency: two threads both try to dispense the LAST available 500 note ==");
        Map<Denomination, Integer> oneNoteLeft = new EnumMap<>(Denomination.class);
        oneNoteLeft.put(Denomination.FIVE_HUNDRED, 1);
        CashDispenser lastNoteDispenser = new CashDispenser(oneNoteLeft);
        CashDispenseStrategy strategy = new LargestNotesFirstStrategy();
        CountDownLatch dispenseLatch = new CountDownLatch(1);
        AtomicReference<String> dispenseResult1 = new AtomicReference<>();
        AtomicReference<String> dispenseResult2 = new AtomicReference<>();
        Thread dispenseThread1 = new Thread(() -> {
            awaitLatch(dispenseLatch);
            try {
                lastNoteDispenser.dispenseCash(500, strategy);
                dispenseResult1.set("DISPENSED");
            } catch (IllegalStateException e) {
                dispenseResult1.set("FAILED: " + e.getMessage());
            }
        });
        Thread dispenseThread2 = new Thread(() -> {
            awaitLatch(dispenseLatch);
            try {
                lastNoteDispenser.dispenseCash(500, strategy);
                dispenseResult2.set("DISPENSED");
            } catch (IllegalStateException e) {
                dispenseResult2.set("FAILED: " + e.getMessage());
            }
        });
        dispenseThread1.start();
        dispenseThread2.start();
        dispenseLatch.countDown();
        dispenseThread1.join();
        dispenseThread2.join();
        System.out.println("Thread 1: " + dispenseResult1.get());
        System.out.println("Thread 2: " + dispenseResult2.get());
        System.out.println("Remaining notes: " + lastNoteDispenser.getNotes()
                + " (must show exactly 0 five-hundreds left - never -1, which would mean the same note was dispensed twice)");
    }

    private static Map<Denomination, Integer> freshNotes() {
        Map<Denomination, Integer> notes = new EnumMap<>(Denomination.class);
        notes.put(Denomination.FIVE_HUNDRED, 10);
        notes.put(Denomination.TWO_HUNDRED, 10);
        notes.put(Denomination.HUNDRED, 10);
        return notes;
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
