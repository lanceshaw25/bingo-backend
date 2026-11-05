package dev.lshaw.bingo.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * The 25 squares dealt to a player: five columns (B, I, N, G, O), each holding numbers from its
 * {@link Column} range, with a free square in the center.
 *
 * <p>A card is immutable and holds only its numbers, not which squares a player has marked.
 * Marking is a client concern; the server decides a win from the card and the game's called
 * numbers. Every card is valid: the constructor rejects any grid that breaks the rules.
 *
 * <p>Squares are addressed as {@code (row, col)}, both zero-based. Column index 0 is B and 4 is O.
 */
public final class BingoCard {

	public static final int SIZE = 5;

	/** The value of the free center square. Never a real Bingo number. */
	public static final int FREE = 0;

	private static final int CENTER = SIZE / 2;
	private static final Column[] COLUMNS = Column.values();

	private final int[][] grid;

	/**
	 * Creates a card from a grid indexed {@code [row][col]}. The grid is copied, so later changes
	 * to the caller's array do not affect the card.
	 *
	 * @throws IllegalArgumentException if the grid is not 5 x 5, a number is outside its column's
	 *     range, a number appears twice, or the center is not {@link #FREE}
	 */
	public BingoCard(int[][] grid) {
		Objects.requireNonNull(grid, "grid");
		// Copy before validating, so the caller cannot change the grid after it has been checked.
		this.grid = copyOf(grid);
		validate(this.grid);
	}

	/**
	 * Deals a random card. Pass a seeded generator to get the same card every time.
	 */
	public static BingoCard random(RandomGenerator rng) {
		Objects.requireNonNull(rng, "rng");
		int[][] grid = new int[SIZE][SIZE];

		for (int col = 0; col < SIZE; col++) {
			Column column = COLUMNS[col];
			List<Integer> numbers = new ArrayList<>();
			for (int n = column.min(); n <= column.max(); n++) {
				numbers.add(n);
			}
			Collections.shuffle(numbers, rng);

			int next = 0;
			for (int row = 0; row < SIZE; row++) {
				grid[row][col] = isCenter(row, col) ? FREE : numbers.get(next++);
			}
		}
		return new BingoCard(grid);
	}

	/**
	 * Returns the number at a square, or {@link #FREE} for the center.
	 *
	 * @throws IndexOutOfBoundsException if row or col is not 0-4
	 */
	public int numberAt(int row, int col) {
		Objects.checkIndex(row, SIZE);
		Objects.checkIndex(col, SIZE);
		return grid[row][col];
	}

	/**
	 * @throws IndexOutOfBoundsException if row or col is not 0-4
	 */
	public boolean isFree(int row, int col) {
		return numberAt(row, col) == FREE;
	}

	/** Whether the number is on this card. The free square is not a number, so {@code contains(FREE)} is false. */
	public boolean contains(int number) {
		if (number == FREE) {
			return false;
		}
		for (int[] row : grid) {
			for (int n : row) {
				if (n == number) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BingoCard other && Arrays.deepEquals(grid, other.grid);
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(grid);
	}

	/** The card as a printed grid, with {@code *} for the free square. Intended for logs and tests. */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Column column : COLUMNS) {
			sb.append("%3s".formatted(column));
		}
		for (int[] row : grid) {
			sb.append('\n');
			for (int n : row) {
				sb.append("%3s".formatted(n == FREE ? "*" : n));
			}
		}
		return sb.toString();
	}

	private static boolean isCenter(int row, int col) {
		return row == CENTER && col == CENTER;
	}

	private static int[][] copyOf(int[][] source) {
		if (source.length != SIZE) {
			throw new IllegalArgumentException("A card must have %d rows, but had %d".formatted(SIZE, source.length));
		}
		int[][] copy = new int[SIZE][];
		for (int row = 0; row < SIZE; row++) {
			int[] r = source[row];
			if (r == null || r.length != SIZE) {
				throw new IllegalArgumentException("Row %d must have %d numbers, but had %s"
						.formatted(row, SIZE, r == null ? "null" : r.length));
			}
			copy[row] = r.clone();
		}
		return copy;
	}

	private static void validate(int[][] grid) {
		Set<Integer> seen = new HashSet<>();
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int n = grid[row][col];

				if (isCenter(row, col)) {
					if (n != FREE) {
						throw new IllegalArgumentException("The center square must be FREE (%d), but was %d"
								.formatted(FREE, n));
					}
					continue;
				}

				// This also rejects FREE anywhere but the center, since 0 is in no column's range.
				Column column = COLUMNS[col];
				if (!column.contains(n)) {
					throw new IllegalArgumentException("Square (%d, %d) is in column %s and must be %d-%d, but was %d"
							.formatted(row, col, column, column.min(), column.max(), n));
				}
				if (!seen.add(n)) {
					throw new IllegalArgumentException("Number %d appears more than once".formatted(n));
				}
			}
		}
	}
}
