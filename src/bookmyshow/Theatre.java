package bookmyshow;

import java.util.ArrayList;
import java.util.List;

public class Theatre {
    private final String id;
    private final String name;
    private final String address;
    private final List<Screen> screens = new ArrayList<>();

    public Theatre(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public void addScreen(Screen screen) {
        screens.add(screen);
    }

    public List<Screen> getScreens() {
        return screens;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
