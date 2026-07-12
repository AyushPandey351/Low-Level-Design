package tictactoe;

// IN_PROGRESS is the default/starting state; Game transitions to WIN or DRAW and then
// stops accepting moves. Modeling this as an enum (rather than e.g. a boolean
// "gameOver" flag) matters once you need to distinguish DRAW from WIN - a single
// boolean can't tell you WHICH ending happened, just that the game ended.
public enum GameStatus {
    IN_PROGRESS,
    WIN,
    DRAW
}
