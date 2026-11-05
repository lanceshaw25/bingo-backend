package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ColumnTest {

	@ParameterizedTest
	@CsvSource({
			"1, B", "15, B",
			"16, I", "30, I",
			"31, N", "45, N",
			"46, G", "60, G",
			"61, O", "75, O"
	})
	void forNumberFindsColumnAtRangeEdges(int number, Column expected) {
		assertThat(Column.forNumber(number)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 76})
	void forNumberRejectsNumbersOutsideOneToSeventyFive(int number) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Column.forNumber(number))
				.withMessageContaining(String.valueOf(number));
	}

	@Test
	void columnsCoverOneToSeventyFiveWithFifteenNumbersEach() {
		int expectedMin = Column.LOWEST_NUMBER;
		for (Column column : Column.values()) {
			assertThat(column.min()).as("min of %s", column).isEqualTo(expectedMin);
			assertThat(column.max() - column.min() + 1).as("size of %s", column).isEqualTo(15);
			expectedMin = column.max() + 1;
		}
		assertThat(expectedMin - 1).isEqualTo(Column.HIGHEST_NUMBER);
	}
}
