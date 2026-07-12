package tictactoe;

// Plain data-holder, same reasoning as User in the Splitwise design: even though your
// notes list makeMove() as a Player behavior, the actual logic of validating and
// applying a move belongs to Game/Board (they own the board state and turn rules) -
// if Player applied moves to the board itself, it would need a reference back to
// Game/Board, creating the same kind of circular dependency User would have had with
// ExpenseManager. Game.makeMove(player, row, col) is where that behavior actually lives.
//
// Kept as a concrete class (not abstract) because both players in this design are
// identical in behavior - only their symbol differs. If you later add an AI player
// mode (per the "allow more game modes later" assumption), that's the natural point
// to introduce a Player hierarchy (e.g. an abstract decideMove(Board) method,
// implemented differently by HumanPlayer vs AIPlayer) - not needed yet, so we don't
// build it until a second, genuinely different kind of player actually shows up.
public class Player {
    private final String id;
    private final String name;
    private final Symbol symbol;

    public Player(String id, String name, Symbol symbol) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}
