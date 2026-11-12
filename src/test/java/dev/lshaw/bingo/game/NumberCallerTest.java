package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumberCallerTest {

	private static List<Integer> callAll(NumberCaller caller) {
		List<Integer> calls = new ArrayList<>();
		while (caller.hasNext()) {
			calls.add(caller.callNext());
		}
		return calls;
	}

	@Test
	void newCallerHasCalledNothing() {
		NumberCaller caller = new NumberCaller(new Random(1));
		assertThat(caller.hasNext()).isTrue();
		assertThat(caller.calledCount()).isZero();
		assertThat(caller.calledNumbers()).isEmpty();
	}

	@Test
	void callsEveryNumberExactlyOnce() {
		List<Integer> calls = callAll(new NumberCaller(new Random(1)));
		List<Integer> oneToSeventyFive = IntStream.rangeClosed(1, 75).boxed().toList();
		assertThat(calls).hasSize(75).containsExactlyInAnyOrderElementsOf(oneToSeventyFive);
	}

	@Test
	void callsEveryNumberExactlyOnceForManySeeds() {
		for (long seed = 0; seed < 200; seed++) {
			assertThat(callAll(new NumberCaller(new Random(seed)))).as("seed %d", seed).doesNotHaveDuplicates().hasSize(75);
		}
	}

	@Test
	void callingAfterTheLastNumberThrows() {
		NumberCaller caller = new NumberCaller(new Random(1));
		callAll(caller);
		assertThat(caller.hasNext()).isFalse();
		assertThatIllegalStateException().isThrownBy(caller::callNext).withMessageContaining("75");
	}

	@Test
	void sameSeedCallsSameOrder() {
		assertThat(callAll(new NumberCaller(new Random(42)))).isEqualTo(callAll(new NumberCaller(new Random(42))));
	}

	@Test
	void differentSeedsCallDifferentOrders() {
		assertThat(callAll(new NumberCaller(new Random(42)))).isNotEqualTo(callAll(new NumberCaller(new Random(43))));
	}

	@Test
	void orderIsNotSimplyOneToSeventyFive() {
		List<Integer> calls = callAll(new NumberCaller(new Random(1)));
		assertThat(calls).isNotEqualTo(IntStream.rangeClosed(1, 75).boxed().toList());
	}

	@Test
	void isCalledTracksCalledNumbers() {
		NumberCaller caller = new NumberCaller(new Random(1));
		int first = caller.callNext();
		int notYetCalled = callAll(new NumberCaller(new Random(1))).get(1);

		assertThat(caller.isCalled(first)).isTrue();
		assertThat(caller.isCalled(notYetCalled)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, BingoCard.FREE, 76})
	void isCalledRejectsNumbersOutsideOneToSeventyFive(int number) {
		NumberCaller caller = new NumberCaller(new Random(1));
		assertThatIllegalArgumentException().isThrownBy(() -> caller.isCalled(number));
	}

	@Test
	void calledNumbersAreInCallOrder() {
		NumberCaller caller = new NumberCaller(new Random(1));
		int a = caller.callNext();
		int b = caller.callNext();
		int c = caller.callNext();

		assertThat(caller.calledNumbers()).containsExactly(a, b, c);
		assertThat(caller.calledCount()).isEqualTo(3);
	}

	@Test
	void calledNumbersIsASnapshot() {
		NumberCaller caller = new NumberCaller(new Random(1));
		caller.callNext();
		List<Integer> snapshot = caller.calledNumbers();
		caller.callNext();

		assertThat(snapshot).hasSize(1);
	}

	@Test
	void calledNumbersCannotBeModified() {
		NumberCaller caller = new NumberCaller(new Random(1));
		caller.callNext();
		assertThatThrownBy(() -> caller.calledNumbers().add(99)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsNullRandomGenerator() {
		assertThatThrownBy(() -> new NumberCaller(null)).isInstanceOf(NullPointerException.class);
	}
}
