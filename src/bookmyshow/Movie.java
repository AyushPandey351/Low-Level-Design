package bookmyshow;

import java.time.Duration;

// Plain data-holder. getShows() from the notes is deliberately NOT implemented here -
// same reasoning as every other data class in this series (User, Player, Customer):
// "all shows for this movie" is a query across a shared show registry, which would
// need Movie to hold a back-reference to that registry. It lives on MovieCatalog
// instead (see that class), which owns the search/browse side of this design.
public class Movie {
    private final String id;
    private final String name;
    private final Duration duration;
    private final String language;

    public Movie(String id, String name, Duration duration, String language) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.language = language;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return name + " (" + language + ")";
    }
}
