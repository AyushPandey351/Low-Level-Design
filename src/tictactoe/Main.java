package tictactoe;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<WinningStrategy> strategies = Arrays.asList(
                new RowWinningStrategy(),
                new ColumnWinningStrategy(),
                new DiagonalWinningStrategy());

        Player ayush = new Player("P1", "Ayush", Symbol.X);
        Player rahul = new Player("P2", "Rahul", Symbol.O);

        // --- Game 1: Step 6 walkthrough - Ayush completes the top row ---
        System.out.println("== Game 1: row win ==");
        Game game1 = new Game(3, Arrays.asList(ayush, rahul), strategies);
        int[][] moves1 = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}};
        playScripted(game1, moves1);
        game1.getBoard().displayBoard();
        game1.displayResult();

        // --- Game 2: diagonal win ---
        System.out.println("\n== Game 2: diagonal win ==");
        Game game2 = new Game(3, Arrays.asList(ayush, rahul), strategies);
        int[][] moves2 = {{0, 0}, {0, 1}, {1, 1}, {0, 2}, {2, 2}};
        playScripted(game2, moves2);
        game2.getBoard().displayBoard();
        game2.displayResult();

        // --- Game 3: full board, nobody wins ---
        System.out.println("\n== Game 3: draw ==");
        Game game3 = new Game(3, Arrays.asList(ayush, rahul), strategies);
        int[][] moves3 = {
                {0, 0}, {0, 1}, {1, 1}, {2, 0}, {0, 2}, {1, 2}, {1, 0}, {2, 2}, {2, 1}
        };
        playScripted(game3, moves3);
        game3.getBoard().displayBoard();
        game3.displayResult();
    }

    // Feeds a scripted list of (row, col) moves into the game, alternating turns.
    // Stops early if the game ends before all scripted moves are used.
    private static void playScripted(Game game, int[][] moves) {
        for (int[] move : moves) {
            if (game.getStatus() != GameStatus.IN_PROGRESS) {
                break;
            }
            Player mover = game.getCurrentPlayer();
            game.makeMove(move[0], move[1]);
            System.out.println(mover.getName() + " plays (" + move[0] + "," + move[1] + ")");
        }
    }
}
