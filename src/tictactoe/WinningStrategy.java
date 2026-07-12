package tictactoe;

// Strategy Pattern, same shape as SplitStrategy/PricingStrategy in the earlier
// designs - Game holds a List<WinningStrategy> (the interface), never concrete
// RowWinningStrategy/etc. directly.
//
// checkWinner(board, move) is deliberately given the LATEST move, not just the board.
// This is the key efficiency idea from your notes: a naive design rescans the ENTIRE
// board every turn (all rows, all columns, both diagonals - O(n^2) work per move just
// to check "did anyone win yet?"). Since a move can only possibly complete a line that
// PASSES THROUGH the cell just played, each strategy only needs to inspect the one
// row, one column, or one/two diagonals touching that cell - O(n) work instead of
// O(n^2). Multiply by however many strategies are configured, and it's still far
// cheaper than a full-board rescan.
public interface WinningStrategy {
    boolean checkWinner(Board board, Move move);
}
