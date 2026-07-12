package bookmyshow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Homes the two "browse" APIs from Step 5 (searchMovie, getShows) that don't have a
// natural home on Movie itself (see Movie's comment on why getShows() isn't there).
// Not one of your explicitly listed classes, but a small, honest home for two
// explicitly listed APIs - the alternative would be forcing Movie to hold a
// back-reference to every Show that's ever scheduled for it, which is the exact
// circular-dependency problem this series has avoided in every prior design.
public class MovieCatalog {
    private final List<Show> shows = new ArrayList<>();

    public void addShow(Show show) {
        shows.add(show);
    }

    public List<Movie> searchMovie(String name) {
        return shows.stream()
                .map(Show::getMovie)
                .filter(movie -> movie.getName().equalsIgnoreCase(name))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Show> getShows(Movie movie) {
        return shows.stream()
                .filter(show -> show.getMovie().equals(movie))
                .collect(Collectors.toList());
    }
}
