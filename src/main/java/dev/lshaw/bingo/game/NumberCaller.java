package dev.lshaw.bingo.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import static dev.lshaw.bingo.game.Column.HIGHEST_NUMBER;
import static dev.lshaw.bingo.game.Column.LOWEST_NUMBER;

/**
 * Calls the numbers 1-75 for one game in a random order, never repeating a number.
 *
 * <p>The order is shuffled once, when the caller is created, and numbers are handed out one at a
 * time by {@link #callNext()}.
 *
 * <p>Not thread-safe. The game that owns the caller is responsible for synchronizing access,
 * because calling a number and publishing it need to happen together as one step.
 */
public final class NumberCaller {

	private final int[] order;
	private final boolean[] called = new boolean[HIGHEST_NUMBER + 1];
	private int calledCount = 0;

	/**
	 * Creates a caller with a shuffled order. Pass a seeded generator to get the same order every time.
	 */
	public NumberCaller(RandomGenerator rng) {
		Objects.requireNonNull(rng, "rng");
		List<Integer> numbers = new ArrayList<>();
		for (int n = LOWEST_NUMBER; n <= HIGHEST_NUMBER; n++) {
			numbers.add(n);
		}
		Collections.shuffle(numbers, rng);
		this.order = numbers.stream().mapToInt(Integer::intValue).toArray();
	}

	/** Whether any numbers are left to call. */
	public boolean hasNext() {
		return calledCount < order.length;
	}

	/**
	 * Calls the next number.
	 *
	 * @throws IllegalStateException if all 75 numbers have already been called
	 */
	public int callNext() {
		if (!hasNext()) {
			throw new IllegalStateException("All %d numbers have been called".formatted(order.length));
		}
		int number = order[calledCount++];
		called[number] = true;
		return number;
	}

	/**
	 * Whether the number has been called.
	 *
	 * @throws IllegalArgumentException if the number is not between 1 and 75. This includes
	 *     {@link BingoCard#FREE}, which is never called.
	 */
	public boolean isCalled(int number) {
		if (number < LOWEST_NUMBER || number > HIGHEST_NUMBER) {
			throw new IllegalArgumentException("Bingo numbers are %d-%d, but got %d"
					.formatted(LOWEST_NUMBER, HIGHEST_NUMBER, number));
		}
		return called[number];
	}

	/** How many numbers have been called so far. */
	public int calledCount() {
		return calledCount;
	}

	/**
	 * The numbers called so far, in the order they were called. The list is an unmodifiable copy,
	 * so it does not change when more numbers are called.
	 */
	public List<Integer> calledNumbers() {
		return Arrays.stream(order, 0, calledCount).boxed().toList();
	}
}
