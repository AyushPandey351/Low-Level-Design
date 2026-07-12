package tictactoe;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// Coordinator, same role as ExpenseManager/ParkingLot in the earlier designs. Holds
// List<WinningStrategy> (per your "Improvement" note) rather than one monolithic
// checkWinner() - to check "only rows matter" or add "4-in-a-row" as a NEW rule,
// you add/remove entries in this list; makeMove() itself never changes. That's a
// stronger OCP demonstration than a single WinningStrategy with all the logic inside
// one method, because here the RULES themselves are swappable, not just their
// implementation.
//
// players is a Deque used as a rotating queue: pollFirst() gets whoever's turn it is,
// and if the game continues, addLast() puts them back at the end - so "switch player"
// is just "move the front player to the back," which trivially generalizes beyond two
// players (a 3+ player variant needs zero changes to this rotation logic).
public class Game {
    private final Board board;
    private final Deque<Player> players;
    private final List<WinningStrategy> winningStrategies;
    private GameStatus status;
    private Player winner;

    public Game(int boardSize, List<Player> players, List<WinningStrategy> winningStrategies) {
        this.board = new Board(boardSize);
        this.players = new ArrayDeque<>(players);
        this.winningStrategies = winningStrategies;
        this.status = GameStatus.IN_PROGRESS;
    }

    public void startGame() {
        this.status = GameStatus.IN_PROGRESS;
    }

    public void makeMove(int row, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game has already ended");
        }

        Player currentPlayer = players.peekFirst();
        board.placeMove(row, col, currentPlayer.getSymbol());
        Move move = new Move(currentPlayer, row, col);

        if (checkWinner(move)) {
            status = GameStatus.WIN;
            winner = currentPlayer;
            return;
        }

        if (board.isBoardFull()) {
            status = GameStatus.DRAW;
            return;
        }

        // Turn continues: move current player to the back of the queue.
        players.addLast(players.pollFirst());
    }

    private boolean checkWinner(Move move) {
        for (WinningStrategy strategy : winningStrategies) {
            if (strategy.checkWinner(board, move)) {
                return true;
            }
        }
        return false;
    }

    public void displayResult() {
        if (status == GameStatus.WIN) {
            System.out.println("Winner: " + winner);
        } else if (status == GameStatus.DRAW) {
            System.out.println("Game ended in a draw.");
        } else {
            System.out.println("Game still in progress.");
        }
    }

    public Board getBoard() {
        return board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getCurrentPlayer() {
        return players.peekFirst();
    }
}
