package dev.lshaw.bingo.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** The events a {@link Game} reports to its listener, and their sequence numbers. */
class GameEventsTest {

	private final List<GameEvent> events = new ArrayList<>();
	private Game game;

	@BeforeEach
	void setUp() {
		game = new Game(GameId.random(), new Random(1), events::add);
	}

	private void callUntilBingo(Player player) {
		while (!WinChecker.hasBingo(player.card(), game.snapshot().calledNumbers()::contains)) {
			game.callNextNumber();
		}
	}

	private GameEvent lastEvent() {
		return events.getLast();
	}

	@Test
	void creatingAGameIsEventOne() {
		assertThat(events).containsExactly(new GameEvent.GameCreated(game.id(), 1));
		assertThat(game.snapshot().sequence()).isEqualTo(1);
	}

	@Test
	void aWholeGameEmitsEventsInOrderWithConsecutiveSequences() {
		Player lance = game.join("Lance");
		game.join("Bot");
		game.start();
		callUntilBingo(lance);
		ClaimResult.Won won = (ClaimResult.Won) game.claimBingo(lance.id());

		int numbersCalled = game.snapshot().calledNumbers().size();
		assertThat(events).hasSize(4 + numbersCalled + 1);
		assertThat(events).extracting(GameEvent::sequence)
				.containsExactlyElementsOf(LongStream.rangeClosed(1, events.size()).boxed().toList());
		assertThat(events.subList(0, 4)).extracting(e -> e.getClass().getSimpleName())
				.containsExactly("GameCreated", "PlayerJoined", "PlayerJoined", "GameStarted");
		assertThat(events.subList(4, 4 + numbersCalled)).allMatch(e -> e instanceof GameEvent.NumberCalled);
		assertThat(lastEvent()).isEqualTo(new GameEvent.GameWon(game.id(), events.size(), lance.id(), "Lance", won.lines()));
		assertThat(game.snapshot().sequence()).isEqualTo(events.size());
	}

	@Test
	void playerJoinedCarriesTheTrimmedName() {
		Player player = game.join("  Lance ");
		assertThat(lastEvent()).isEqualTo(new GameEvent.PlayerJoined(game.id(), 2, player.id(), "Lance"));
	}

	@Test
	void gameStartedListsPlayersInJoinOrder() {
		game.join("Lance");
		game.join("Bot");
		game.start();
		assertThat(lastEvent()).isEqualTo(new GameEvent.GameStarted(game.id(), 4, List.of("Lance", "Bot")));
	}

	@Test
	void numberCalledCarriesTheNumberAndCount() {
		game.join("Lance");
		game.start();
		int first = game.callNextNumber();
		int second = game.tryCallNextNumber().orElseThrow();

		assertThat(events.subList(3, 5)).containsExactly(
				new GameEvent.NumberCalled(game.id(), 4, first, 1),
				new GameEvent.NumberCalled(game.id(), 5, second, 2));
	}

	@Test
	void snapshotSequenceMatchesTheLastEvent() {
		game.join("Lance");
		game.start();
		game.callNextNumber();
		assertThat(game.snapshot().sequence()).isEqualTo(lastEvent().sequence()).isEqualTo(4);
	}

	@Test
	void rejectedActionsEmitNothing() {
		assertThatIllegalStateException().isThrownBy(game::start);
		assertThatIllegalStateException().isThrownBy(game::callNextNumber);
		assertThat(game.tryCallNextNumber()).isEmpty();
		Player lance = game.join("Lance");
		assertThatIllegalArgumentException().isThrownBy(() -> game.join("LANCE"));
		assertThatIllegalArgumentException().isThrownBy(() -> game.join(" "));
		assertThat(game.claimBingo(lance.id())).isInstanceOf(ClaimResult.GameNotInProgress.class);
		game.start();
		assertThat(game.claimBingo(lance.id())).isInstanceOf(ClaimResult.NoBingo.class);

		assertThat(events).extracting(e -> e.getClass().getSimpleName())
				.containsExactly("GameCreated", "PlayerJoined", "GameStarted");
	}

	@Test
	void nothingAfterTheGameIsWon() {
		Player lance = game.join("Lance");
		Player bot = game.join("Bot");
		game.start();
		callUntilBingo(lance);
		game.claimBingo(lance.id());
		int count = events.size();

		assertThat(game.tryCallNextNumber()).isEmpty();
		assertThat(game.claimBingo(bot.id())).isInstanceOf(ClaimResult.GameNotInProgress.class);
		assertThat(events).hasSize(count);
	}

	@Test
	void listenerIsCalledWhileHoldingTheGamesLock() {
		List<Boolean> heldLock = new ArrayList<>();
		Game[] holder = new Game[1];
		holder[0] = new Game(GameId.random(), new Random(1), event -> {
			if (holder[0] != null) {
				heldLock.add(Thread.holdsLock(holder[0]));
			}
		});
		Game locked = holder[0];
		locked.join("Lance");
		locked.start();
		locked.callNextNumber();

		assertThat(heldLock).hasSize(3).containsOnly(true);
	}
}
