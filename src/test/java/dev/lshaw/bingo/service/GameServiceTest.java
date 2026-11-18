package dev.lshaw.bingo.service;

import dev.lshaw.bingo.game.ClaimResult;
import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.GameSnapshot;
import dev.lshaw.bingo.game.GameStatus;
import dev.lshaw.bingo.game.Player;
import dev.lshaw.bingo.game.WinChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

	private static final Duration INTERVAL = Duration.ofSeconds(3);

	private ManualTaskScheduler scheduler;
	private GameService service;

	@BeforeEach
	void setUp() {
		scheduler = new ManualTaskScheduler();
		service = new GameService(scheduler, new Random(1), new BingoProperties(INTERVAL));
	}

	private GameId startedGameWith(String... names) {
		GameId id = service.createGame().id();
		for (String name : names) {
			service.join(id, name);
		}
		service.start(id);
		return id;
	}

	private int calledCount(GameId id) {
		return service.snapshot(id).calledNumbers().size();
	}

	/** Lets the scheduler call numbers until the player's card has a complete line. */
	private void tickUntilBingo(GameId id, Player player) {
		while (!WinChecker.hasBingo(player.card(), service.snapshot(id).calledNumbers()::contains)) {
			scheduler.tick();
		}
	}

	// --- Creating and finding games ---

	@Test
	void createdGameIsWaitingForPlayers() {
		GameSnapshot created = service.createGame();
		assertThat(created.status()).isEqualTo(GameStatus.WAITING_FOR_PLAYERS);
		assertThat(service.snapshot(created.id())).isEqualTo(created);
	}

	@Test
	void gamesListsEveryGame() {
		GameId a = service.createGame().id();
		GameId b = service.createGame().id();
		assertThat(service.games()).extracting(GameSnapshot::id).containsExactlyInAnyOrder(a, b);
	}

	@Test
	void unknownGameIsNotFound() {
		GameId missing = GameId.random();
		assertThatThrownBy(() -> service.snapshot(missing)).isInstanceOf(GameNotFoundException.class);
		assertThatThrownBy(() -> service.join(missing, "Lance")).isInstanceOf(GameNotFoundException.class);
		assertThatThrownBy(() -> service.start(missing)).isInstanceOf(GameNotFoundException.class);
		assertThatThrownBy(() -> service.claimBingo(missing, null)).isInstanceOf(GameNotFoundException.class);
	}

	@Test
	void gameNotFoundNamesTheGame() {
		GameId missing = GameId.random();
		assertThatThrownBy(() -> service.snapshot(missing))
				.isInstanceOfSatisfying(GameNotFoundException.class, e -> assertThat(e.gameId()).isEqualTo(missing))
				.hasMessageContaining(missing.toString());
	}

	// --- Delegating to the game ---

	@Test
	void joinAddsThePlayer() {
		GameId id = service.createGame().id();
		Player lance = service.join(id, "Lance");
		assertThat(service.snapshot(id).players()).containsExactly(lance);
	}

	@Test
	void gameRuleErrorsPassThrough() {
		GameId id = service.createGame().id();
		assertThatIllegalArgumentException().isThrownBy(() -> service.join(id, " "));
		assertThatIllegalStateException().isThrownBy(() -> service.start(id)); // no players
	}

	// --- Calling numbers ---

	@Test
	void startSchedulesCallingOneIntervalFromNow() {
		GameId id = startedGameWith("Lance");

		assertThat(scheduler.tasks()).singleElement().satisfies(task -> {
			assertThat(task.startTime()).isEqualTo(ManualTaskScheduler.NOW.plus(INTERVAL));
			assertThat(task.delay()).isEqualTo(INTERVAL);
		});
		assertThat(calledCount(id)).as("nothing called until the first interval passes").isZero();
	}

	@Test
	void eachTickCallsOneNumber() {
		GameId id = startedGameWith("Lance");
		scheduler.tick(3);
		assertThat(calledCount(id)).isEqualTo(3);
	}

	@Test
	void failedStartSchedulesNothing() {
		GameId id = service.createGame().id();
		assertThatIllegalStateException().isThrownBy(() -> service.start(id));
		assertThat(scheduler.tasks()).isEmpty();
	}

	@Test
	void callingStopsAfterAllSeventyFiveNumbers() {
		GameId id = startedGameWith("Lance");
		scheduler.tick(80);

		assertThat(calledCount(id)).isEqualTo(75);
		assertThat(scheduler.activeTasks()).isEmpty();
		assertThat(service.snapshot(id).status()).as("still open for claims").isEqualTo(GameStatus.IN_PROGRESS);
	}

	@Test
	void eachGameHasItsOwnCalling() {
		GameId a = startedGameWith("Ann");
		GameId b = startedGameWith("Bob");
		scheduler.tick(2);

		assertThat(scheduler.activeTasks()).hasSize(2);
		assertThat(calledCount(a)).isEqualTo(2);
		assertThat(calledCount(b)).isEqualTo(2);
	}

	// --- Claiming ---

	@Test
	void winningClaimStopsCalling() {
		GameId id = service.createGame().id();
		Player lance = service.join(id, "Lance");
		service.start(id);
		tickUntilBingo(id, lance);

		assertThat(service.claimBingo(id, lance.id())).isInstanceOf(ClaimResult.Won.class);
		assertThat(scheduler.activeTasks()).isEmpty();

		int calledAtWin = calledCount(id);
		scheduler.tick();
		assertThat(calledCount(id)).isEqualTo(calledAtWin);
	}

	@Test
	void losingClaimKeepsCalling() {
		GameId id = service.createGame().id();
		Player lance = service.join(id, "Lance");
		service.start(id);
		scheduler.tick();

		assertThat(service.claimBingo(id, lance.id())).isInstanceOf(ClaimResult.NoBingo.class);
		assertThat(scheduler.activeTasks()).hasSize(1);
	}

	@Test
	void winningOneGameDoesNotStopAnother() {
		GameId a = service.createGame().id();
		Player ann = service.join(a, "Ann");
		service.start(a);
		GameId b = startedGameWith("Bob");

		tickUntilBingo(a, ann);
		service.claimBingo(a, ann.id());

		int bCalls = calledCount(b);
		scheduler.tick();
		assertThat(calledCount(b)).isEqualTo(bCalls + 1);
	}

	// --- Properties ---

	@Test
	void callIntervalMustBePositive() {
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoProperties(Duration.ZERO));
		assertThatIllegalArgumentException().isThrownBy(() -> new BingoProperties(Duration.ofSeconds(-1)));
	}
}
