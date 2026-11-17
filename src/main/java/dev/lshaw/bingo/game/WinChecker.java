package dev.lshaw.bingo.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Decides whether a card has won, given which numbers have been called.
 *
 * <p>A square is marked if it is the free center square or its number has been called. Called
 * numbers are passed as a predicate, so this works with {@code caller::isCalled} on the server or
 * {@code calledSet::contains} anywhere else. The predicate is never asked about
 * {@link BingoCard#FREE}.
 */
public final class WinChecker {

	private WinChecker() {
	}

	/** Whether the card has at least one complete winning line. */
	public static boolean hasBingo(BingoCard card, IntPredicate isCalled) {
		return !winningLines(card, isCalled).isEmpty();
	}

	/**
	 * Every complete winning line on the card, in {@link WinningLine} declaration order. Empty if
	 * the card has not won.
	 */
	public static List<WinningLine> winningLines(BingoCard card, IntPredicate isCalled) {
		Objects.requireNonNull(card, "card");
		Objects.requireNonNull(isCalled, "isCalled");

		List<WinningLine> lines = new ArrayList<>();
		for (WinningLine line : WinningLine.values()) {
			if (isComplete(line, card, isCalled)) {
				lines.add(line);
			}
		}
		return List.copyOf(lines);
	}

	private static boolean isComplete(WinningLine line, BingoCard card, IntPredicate isCalled) {
		for (SquarePosition square : line.squares()) {
			if (!isMarked(square, card, isCalled)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isMarked(SquarePosition square, BingoCard card, IntPredicate isCalled) {
		// Check FREE first, so the predicate is only ever asked about real numbers.
		return card.isFree(square.row(), square.col())
				|| isCalled.test(card.numberAt(square.row(), square.col()));
	}
}
