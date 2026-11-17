package dev.lshaw.bingo.game;

import java.util.ArrayList;
import java.util.List;

import static dev.lshaw.bingo.game.BingoCard.SIZE;

/**
 * The lines that win a game: any full row, column, or diagonal. The free center square counts
 * as marked, so the middle row, middle column, and both diagonals need only four called numbers.
 *
 * <p>To add a pattern such as four corners, add a constant with its squares.
 */
public enum WinningLine {
	ROW_1(row(0)),
	ROW_2(row(1)),
	ROW_3(row(2)),
	ROW_4(row(3)),
	ROW_5(row(4)),
	COLUMN_B(column(0)),
	COLUMN_I(column(1)),
	COLUMN_N(column(2)),
	COLUMN_G(column(3)),
	COLUMN_O(column(4)),
	/** Top left to bottom right. */
	DIAGONAL_DOWN(diagonalDown()),
	/** Bottom left to top right. */
	DIAGONAL_UP(diagonalUp());

	private final List<SquarePosition> squares;

	WinningLine(List<SquarePosition> squares) {
		this.squares = List.copyOf(squares);
	}

	/** The squares that must all be marked to win with this line. */
	public List<SquarePosition> squares() {
		return squares;
	}

	private static List<SquarePosition> row(int row) {
		List<SquarePosition> squares = new ArrayList<>();
		for (int col = 0; col < SIZE; col++) {
			squares.add(new SquarePosition(row, col));
		}
		return squares;
	}

	private static List<SquarePosition> column(int col) {
		List<SquarePosition> squares = new ArrayList<>();
		for (int row = 0; row < SIZE; row++) {
			squares.add(new SquarePosition(row, col));
		}
		return squares;
	}

	private static List<SquarePosition> diagonalDown() {
		List<SquarePosition> squares = new ArrayList<>();
		for (int i = 0; i < SIZE; i++) {
			squares.add(new SquarePosition(i, i));
		}
		return squares;
	}

	private static List<SquarePosition> diagonalUp() {
		List<SquarePosition> squares = new ArrayList<>();
		for (int i = 0; i < SIZE; i++) {
			squares.add(new SquarePosition(SIZE - 1 - i, i));
		}
		return squares;
	}
}
