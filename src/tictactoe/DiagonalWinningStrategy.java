package tictactoe;

// A move only sits on the main diagonal if row == col, and only on the anti-diagonal
// if row + col == size - 1 (the center cell of an odd-sized board sits on BOTH, which
// is why these are two independent `if` checks, not `if/else` - we must not skip
// checking the anti-diagonal just because the main diagonal check already ran).
// If the move isn't on either diagonal, there's nothing to check at all - most moves
// on a board larger than 3x3 won't be, and we skip the scan entirely rather than
// wastefully checking diagonals a move was never part of.
public class DiagonalWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Move move) {
        int size = board.getSize();
        int row = move.getRow();
        int col = move.getCol();
        Symbol symbol = move.getPlayer().getSymbol();

        if (row == col && checkLine(board, size, symbol, true)) {
            return true;
        }
        if (row + col == size - 1 && checkLine(board, size, symbol, false)) {
            return true;
        }
        return false;
    }

    private boolean checkLine(Board board, int size, Symbol symbol, boolean mainDiagonal) {
        for (int i = 0; i < size; i++) {
            int col = mainDiagonal ? i : size - 1 - i;
            if (board.getCell(i, col).getSymbol() != symbol) {
                return false;
            }
        }
        return true;
    }
}
