package tictactoe;

// Owns the grid and the rules of touching it - Game asks Board to place a move and
// trusts Board to reject invalid ones, rather than Game reaching into a raw
// Cell[][] itself and duplicating validity checks. This mirrors Floor owning its
// ParkingSpot[] in the Parking Lot design: the container class is responsible for
// its own internal collection, not just an exposed array to whoever holds it.
public class Board {
    private final int size;
    private final Cell[][] grid;

    public Board(int size) {
        this.size = size;
        this.grid = new Cell[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    public void placeMove(int row, int col, Symbol symbol) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Cell (" + row + "," + col + ") is out of bounds");
        }
        if (!isCellEmpty(row, col)) {
            throw new IllegalStateException("Cell (" + row + "," + col + ") is already occupied");
        }
        grid[row][col].setSymbol(symbol);
    }

    public boolean isCellEmpty(int row, int col) {
        return grid[row][col].isEmpty();
    }

    public boolean isBoardFull() {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                if (cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    public int getSize() {
        return size;
    }

    public void displayBoard() {
        for (int r = 0; r < size; r++) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < size; c++) {
                Symbol symbol = grid[r][c].getSymbol();
                line.append(symbol == Symbol.EMPTY ? "." : symbol.toString());
                if (c < size - 1) {
                    line.append(" | ");
                }
            }
            System.out.println(line);
        }
    }
}
