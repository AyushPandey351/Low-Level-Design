package atm;

// A real Keypad would block waiting for physical key presses. Since this design's
// demo needs to run deterministically and non-interactively (same reasoning as
// Tic Tac Toe's scripted Main and the earlier designs' simulated inputs), readInput()
// takes the "key presses" as a parameter representing what the customer typed,
// rather than actually blocking on System.in.
public class Keypad {
    public String readInput(String simulatedInput) {
        System.out.println("[Keypad] input: " + simulatedInput);
        return simulatedInput;
    }
}
