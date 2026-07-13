package atm;

import java.util.List;

public class Screen {
    public void displayMessage(String message) {
        System.out.println("[Screen] " + message);
    }

    public void showMenu(List<String> options) {
        System.out.println("[Screen] Menu: " + options);
    }
}
