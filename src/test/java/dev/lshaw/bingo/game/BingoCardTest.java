package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static dev.lshaw.bingo.game.BingoCard.FREE;
import static dev.lshaw.bingo.game.BingoCard.SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BingoCardTest {

	/** A fresh valid grid for each test, so tests can break it without affecting each other. */
	private static int[][] validGrid() {
		return new int[][] {
				{1, 16, 31, 46, 61},
				{2, 17, 32, 47, 62},
				{3, 18, FREE, 48, 63},
				{4, 19, 34, 49, 64},
				{5, 20, 35, 50, 65}
		};
	}

	// --- Random cards ---

	@Test
	void randomCardsFollowTheRules() {
		Random rng = new Random(1);
		for (int i = 0; i < 1_000; i++) {
			assertFollowsRules(BingoCard.random(rng));
		}
	}

	@Test
	void sameSeedDealsSameCard() {
		assertThat(BingoCard.random(new Random(42))).isEqualTo(BingoCard.random(new Random(42)));
	}

	@Test
	void differentSeedsDealDifferentCards() {
		assertThat(BingoCard.random(new Random(42))).isNotEqualTo(BingoCard.random(new Random(43)));
	}

	/**
	 * Checks the rules through the public API, with the column ranges written out independently
	 * of {@link Column}, so this does not just repeat the constructor's own validation.
	 */
	private static void assertFollowsRules(BingoCard card) {
		Set<Integer> numbers = new HashSet<>();
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (row == 2 && col == 2) {
					assertThat(card.isFree(row, col)).as("center of%n%s", card).isTrue();
					continue;
				}
				int n = card.numberAt(row, col);
				int min = 1 + 15 * col;
				int max = 15 * (col + 1);
				assertThat(n).as("square (%d, %d) of%n%s", row, col, card).isBetween(min, max);
				numbers.add(n);
			}
		}
		assertThat(numbers).as("distinct numbers on%n%s", card).hasSize(24);
	}

	// --- Accessors ---

	@Test
	void numberAtReadsRowThenColumn() {
		BingoCard card = new BingoCard(validGrid());
		assertThat(card.numberAt(0, 0)).isEqualTo(1);
		assertThat(card.numberAt(0, 4)).isEqualTo(61);
		assertThat(card.numberAt(4, 0)).isEqualTo(5);
		assertThat(card.numberAt(2, 2)).isEqualTo(FREE);
	}

	@Test
	void onlyTheCenterIsFree() {
		BingoCard card = new BingoCard(validGrid());
		assertThat(card.isFree(2, 2)).isTrue();
		assertThat(card.isFree(0, 0)).isFalse();
	}

	@Test
	void containsFindsNumbersOnTheCard() {
		BingoCard card = new BingoCard(validGrid());
		assertThat(card.contains(1)).isTrue();
		assertThat(card.contains(65)).isTrue();
		assertThat(card.contains(33)).isFalse();
	}

	@Test
	void containsIsFalseForTheFreeSquare() {
		assertThat(new BingoCard(validGrid()).contains(FREE)).isFalse();
	}

	@Test
	void numberAtRejectsSquaresOffTheCard() {
		BingoCard card = new BingoCard(validGrid());
		assertThatThrownBy(() -> card.numberAt(5, 0)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> card.numberAt(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void changingTheSourceGridDoesNotChangeTheCard() {
		int[][] grid = validGrid();
		BingoCard card = new BingoCard(grid);
		grid[0][0] = 99;
		assertThat(card.numberAt(0, 0)).isEqualTo(1);
	}

	@Test
	void cardsWithTheSameNumbersAreEqual() {
		BingoCard a = new BingoCard(validGrid());
		BingoCard b = new BingoCard(validGrid());
		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
	}

	@Test
	void toStringPrintsTheGrid() {
		// Not a text block: text blocks strip the leading spaces that right-align each cell.
		assertThat(new BingoCard(validGrid())).hasToString(String.join("\n",
				"  B  I  N  G  O",
				"  1 16 31 46 61",
				"  2 17 32 47 62",
				"  3 18  * 48 63",
				"  4 19 34 49 64",
				"  5 20 35 50 65"));
	}

	// --- Rejecting invalid grids ---

	@Test
	void rejectsNullGrid() {
		assertThatThrownBy(() -> new BingoCard(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsWrongNumberOfRows() {
		int[][] grid = {validGrid()[0], validGrid()[1], validGrid()[2], validGrid()[3]};
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("rows");
	}

	@Test
	void rejectsShortRow() {
		int[][] grid = validGrid();
		grid[1] = new int[] {2, 17, 32, 47};
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("Row 1");
	}

	@Test
	void rejectsNullRow() {
		int[][] grid = validGrid();
		grid[3] = null;
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("Row 3");
	}

	@Test
	void rejectsNumberInTheWrongColumn() {
		int[][] grid = validGrid();
		grid[0][0] = 16; // an I number in the B column
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("column B");
	}

	@Test
	void rejectsNumberAboveSeventyFive() {
		int[][] grid = validGrid();
		grid[0][4] = 76;
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("column O");
	}

	@Test
	void rejectsDuplicateNumber() {
		int[][] grid = validGrid();
		grid[1][0] = 1;
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("more than once");
	}

	@Test
	void rejectsCenterThatIsNotFree() {
		int[][] grid = validGrid();
		grid[2][2] = 33;
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("center");
	}

	@Test
	void rejectsFreeSquareOutsideTheCenter() {
		int[][] grid = validGrid();
		grid[0][0] = FREE;
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoCard(grid)).withMessageContaining("(0, 0)");
	}
}
