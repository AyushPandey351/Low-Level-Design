package tictactoe;

// Captures ONE move as a self-contained fact: who played, and where. This is what
// gets handed to WinningStrategy.checkWinner(board, move) - each strategy only needs
// to check whether THIS move completed a line, not rescan the entire board from
// scratch every turn (see WinningStrategy's comment for why that matters).
public class Move {
    private final Player player;
    private final int row;
    private final int col;

    public Move(Player player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
