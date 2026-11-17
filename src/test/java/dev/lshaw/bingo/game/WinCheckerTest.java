package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;

import static dev.lshaw.bingo.game.BingoCard.FREE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinCheckerTest {

	private static final BingoCard CARD = new BingoCard(new int[][] {
			{1, 16, 31, 46, 61},
			{2, 17, 32, 47, 62},
			{3, 18, FREE, 48, 63},
			{4, 19, 34, 49, 64},
			{5, 20, 35, 50, 65}
	});

	/** The card's numbers on a line, leaving out the free square. */
	private static Set<Integer> numbersOn(WinningLine line) {
		Set<Integer> numbers = new HashSet<>();
		for (SquarePosition square : line.squares()) {
			if (!CARD.isFree(square.row(), square.col())) {
				numbers.add(CARD.numberAt(square.row(), square.col()));
			}
		}
		return numbers;
	}

	private static IntPredicate called(Set<Integer> numbers) {
		return numbers::contains;
	}

	@Test
	void nothingCalledIsNotAWin() {
		assertThat(WinChecker.hasBingo(CARD, n -> false)).isFalse();
		assertThat(WinChecker.winningLines(CARD, n -> false)).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(WinningLine.class)
	void everyLineWinsOnItsOwn(WinningLine line) {
		IntPredicate isCalled = called(numbersOn(line));
		assertThat(WinChecker.winningLines(CARD, isCalled)).containsExactly(line);
		assertThat(WinChecker.hasBingo(CARD, isCalled)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(WinningLine.class)
	void lineMissingOneNumberDoesNotWin(WinningLine line) {
		Set<Integer> numbers = numbersOn(line);
		numbers.remove(numbers.iterator().next());
		assertThat(WinChecker.hasBingo(CARD, called(numbers))).isFalse();
	}

	@Test
	void freeSquareCountsAsMarked() {
		// The middle row has the free square, so four numbers are enough.
		assertThat(WinChecker.winningLines(CARD, called(Set.of(3, 18, 48, 63)))).containsExactly(WinningLine.ROW_3);
	}

	@Test
	void reportsEveryCompleteLineInDeclarationOrder() {
		Set<Integer> numbers = new HashSet<>(numbersOn(WinningLine.COLUMN_B));
		numbers.addAll(numbersOn(WinningLine.ROW_1));
		assertThat(WinChecker.winningLines(CARD, called(numbers)))
				.containsExactly(WinningLine.ROW_1, WinningLine.COLUMN_B);
	}

	@Test
	void everyLineWinsWhenEveryNumberIsCalled() {
		assertThat(WinChecker.winningLines(CARD, n -> true)).containsExactly(WinningLine.values());
	}

	@Test
	void numbersNotOnTheCardDoNotHelp() {
		// Every number from 1 to 75 except the ones on this card.
		IntPredicate isCalled = n -> !CARD.contains(n);
		assertThat(WinChecker.hasBingo(CARD, isCalled)).isFalse();
	}

	@Test
	void neverAsksAboutTheFreeSquare() {
		IntPredicate isCalled = n -> {
			if (n == FREE) {
				throw new AssertionError("asked about the free square");
			}
			return true;
		};
		assertThat(WinChecker.hasBingo(CARD, isCalled)).isTrue();
	}

	@Test
	void worksWithNumberCaller() {
		// NumberCaller.isCalled throws for FREE, so this also checks the free square is skipped.
		NumberCaller caller = new NumberCaller(new Random(7));
		BingoCard card = BingoCard.random(new Random(7));
		while (!WinChecker.hasBingo(card, caller::isCalled)) {
			caller.callNext();
		}
		List<WinningLine> lines = WinChecker.winningLines(card, caller::isCalled);
		assertThat(lines).isNotEmpty();
		// The last number called must be what completed the win.
		int last = caller.calledNumbers().getLast();
		assertThat(lines).allSatisfy(line -> assertThat(line.squares())
				.anySatisfy(sq -> assertThat(card.numberAt(sq.row(), sq.col())).isEqualTo(last)));
	}

	@Test
	void rejectsNullArguments() {
		assertThatThrownBy(() -> WinChecker.hasBingo(null, n -> true)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> WinChecker.hasBingo(CARD, null)).isInstanceOf(NullPointerException.class);
	}
}
