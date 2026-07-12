package tictactoe;

public class ColumnWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Move move) {
        int col = move.getCol();
        Symbol symbol = move.getPlayer().getSymbol();
        for (int row = 0; row < board.getSize(); row++) {
            if (board.getCell(row, col).getSymbol() != symbol) {
                return false;
            }
        }
        return true;
    }
}
