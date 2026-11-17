package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinningLineTest {

	@Test
	void thereAreTwelveLines() {
		assertThat(WinningLine.values()).hasSize(12);
	}

	@ParameterizedTest
	@EnumSource(WinningLine.class)
	void everyLineHasFiveDistinctSquares(WinningLine line) {
		assertThat(line.squares()).hasSize(5).doesNotHaveDuplicates();
	}

	@Test
	void linesHaveTheExpectedSquares() {
		assertThat(WinningLine.ROW_2.squares()).containsExactly(
				new SquarePosition(1, 0), new SquarePosition(1, 1), new SquarePosition(1, 2), new SquarePosition(1, 3), new SquarePosition(1, 4));
		assertThat(WinningLine.COLUMN_G.squares()).containsExactly(
				new SquarePosition(0, 3), new SquarePosition(1, 3), new SquarePosition(2, 3), new SquarePosition(3, 3), new SquarePosition(4, 3));
		assertThat(WinningLine.DIAGONAL_DOWN.squares()).containsExactly(
				new SquarePosition(0, 0), new SquarePosition(1, 1), new SquarePosition(2, 2), new SquarePosition(3, 3), new SquarePosition(4, 4));
		assertThat(WinningLine.DIAGONAL_UP.squares()).containsExactly(
				new SquarePosition(4, 0), new SquarePosition(3, 1), new SquarePosition(2, 2), new SquarePosition(1, 3), new SquarePosition(0, 4));
	}

	@Test
	void squaresCannotBeModified() {
		assertThatThrownBy(() -> WinningLine.ROW_1.squares().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void squareRejectsPositionsOffTheCard() {
		assertThatThrownBy(() -> new SquarePosition(5, 0)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> new SquarePosition(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
	}
}
