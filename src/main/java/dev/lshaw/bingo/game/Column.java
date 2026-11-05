package dev.lshaw.bingo.game;

/**
 * The five columns of a 75-ball Bingo card and the range of numbers each one holds.
 */
public enum Column {
	B(1, 15),
	I(16, 30),
	N(31, 45),
	G(46, 60),
	O(61, 75);

	public static final int LOWEST_NUMBER = 1;
	public static final int HIGHEST_NUMBER = 75;

	private final int min;
	private final int max;

	Column(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	public boolean contains(int number) {
		return number >= min && number <= max;
	}

	/**
	 * Returns the column a Bingo number belongs to, for example 7 -> B and 52 -> G.
	 *
	 * @throws IllegalArgumentException if the number is not between 1 and 75
	 */
	public static Column forNumber(int number) {
		for (Column column : values()) {
			if (column.contains(number)) {
				return column;
			}
		}
		throw new IllegalArgumentException("Bingo numbers are %d-%d, but got %d"
				.formatted(LOWEST_NUMBER, HIGHEST_NUMBER, number));
	}
}
