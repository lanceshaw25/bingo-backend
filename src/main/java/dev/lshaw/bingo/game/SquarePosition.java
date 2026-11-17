package dev.lshaw.bingo.game;

import java.util.Objects;

/**
 * A position on a {@link BingoCard}. Row and column are zero-based; column 0 is B and 4 is O.
 */
public record SquarePosition(int row, int col) {

	public SquarePosition {
		Objects.checkIndex(row, BingoCard.SIZE);
		Objects.checkIndex(col, BingoCard.SIZE);
	}
}
