package tictactoe;

// The one genuinely mutable piece of state in this whole design - a Cell starts EMPTY
// and gets written to exactly once (Board enforces the "exactly once" part; Cell
// itself only enforces "here's how to read/write my own symbol"). Everything else in
// this design (Move, and Ticket/Split in the earlier designs) represents a fact that
// already happened and is immutable - Cell is different because it represents
// ONGOING state that legitimately changes as the game progresses.
public class Cell {
    private final int row;
    private final int col;
    private Symbol symbol;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.symbol = Symbol.EMPTY;
    }

    public boolean isEmpty() {
        return symbol == Symbol.EMPTY;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
