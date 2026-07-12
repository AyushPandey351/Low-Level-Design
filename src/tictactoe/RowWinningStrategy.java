package tictactoe;

// Only inspects the ONE row the latest move landed in - not all n rows. A move can
// never complete a row it isn't in, so checking the others would be wasted work.
public class RowWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Move move) {
        int row = move.getRow();
        Symbol symbol = move.getPlayer().getSymbol();
        for (int col = 0; col < board.getSize(); col++) {
            if (board.getCell(row, col).getSymbol() != symbol) {
                return false;
            }
        }
        return true;
    }
}
