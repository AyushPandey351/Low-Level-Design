package tictactoe;

// Avoids passing raw chars ('X', 'O') around the codebase - a typo like 'x' vs 'X',
// or confusing 'O' the letter with '0' the digit, becomes a compile-time-checked enum
// value instead of a silent runtime bug. EMPTY is included here (rather than using
// null to mean "no symbol yet") so Cell.isEmpty() can compare against a real value -
// null checks scattered through Board/WinningStrategy are a common source of NPEs
// that a sentinel enum value avoids entirely.
public enum Symbol {
    X,
    O,
    EMPTY
}
