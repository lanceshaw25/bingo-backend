package dev.lshaw.bingo.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static dev.lshaw.bingo.game.GameStatus.FINISHED;
import static dev.lshaw.bingo.game.GameStatus.IN_PROGRESS;
import static dev.lshaw.bingo.game.GameStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class GameTest {

	private static Game newGame() {
		return new Game(GameId.random(), new Random(1));
	}

	/** Calls numbers until the player's card has a complete line, as far as the test can tell. */
	private static void callUntilBingo(Game game, Player player) {
		while (!WinChecker.hasBingo(player.card(), game.snapshot().calledNumbers()::contains)) {
			game.callNextNumber();
		}
	}

	private static void callAll(Game game) {
		while (game.hasNumbersLeft()) {
			game.callNextNumber();
		}
	}

	// --- New game ---

	@Test
	void newGameIsWaitingWithNoPlayers() {
		GameSnapshot snapshot = newGame().snapshot();
		assertThat(snapshot.status()).isEqualTo(WAITING_FOR_PLAYERS);
		assertThat(snapshot.players()).isEmpty();
		assertThat(snapshot.calledNumbers()).isEmpty();
		assertThat(snapshot.winner()).isEmpty();
	}

	// --- Joining ---

	@Test
	void joinAddsPlayerWithACard() {
		Game game = newGame();
		Player lance = game.join("Lance");

		assertThat(lance.name()).isEqualTo("Lance");
		assertThat(lance.card()).isNotNull();
		assertThat(game.player(lance.id())).contains(lance);
		assertThat(game.snapshot().players()).containsExactly(lance);
	}

	@Test
	void playersAreListedInJoinOrder() {
		Game game = newGame();
		Player a = game.join("Ann");
		Player b = game.join("Bob");
		Player c = game.join("Cat");
		assertThat(game.snapshot().players()).containsExactly(a, b, c);
	}

	@Test
	void joinTrimsTheName() {
		assertThat(newGame().join("  Lance  ").name()).isEqualTo("Lance");
	}

	@Test
	void joinRejectsBlankName() {
		Game game = newGame();
		assertThatIllegalArgumentException().isThrownBy(() -> game.join("   "));
	}

	@Test
	void joinRejectsDuplicateNameIgnoringCase() {
		Game game = newGame();
		game.join("Lance");
		assertThatIllegalArgumentException().isThrownBy(() -> game.join("lance")).withMessageContaining("Lance");
	}

	@Test
	void cannotJoinAfterTheGameStarts() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		assertThatIllegalStateException().isThrownBy(() -> game.join("Late"));
	}

	@Test
	void unknownPlayerIsNotFound() {
		assertThat(newGame().player(PlayerId.random())).isEmpty();
	}

	// --- Starting ---

	@Test
	void startMovesTheGameInProgress() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		assertThat(game.status()).isEqualTo(IN_PROGRESS);
	}

	@Test
	void cannotStartWithNoPlayers() {
		assertThatIllegalStateException().isThrownBy(() -> newGame().start()).withMessageContaining("no players");
	}

	@Test
	void cannotStartTwice() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		assertThatIllegalStateException().isThrownBy(game::start);
	}

	// --- Calling numbers ---

	@Test
	void cannotCallNumbersBeforeTheGameStarts() {
		Game game = newGame();
		game.join("Lance");
		assertThatIllegalStateException().isThrownBy(game::callNextNumber);
	}

	@Test
	void calledNumbersAppearInTheSnapshotInOrder() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		int first = game.callNextNumber();
		int second = game.callNextNumber();
		assertThat(game.snapshot().calledNumbers()).containsExactly(first, second);
	}

	@Test
	void cannotCallMoreThanSeventyFiveNumbers() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		callAll(game);

		assertThat(game.snapshot().calledNumbers()).hasSize(75);
		assertThat(game.hasNumbersLeft()).isFalse();
		assertThatIllegalStateException().isThrownBy(game::callNextNumber);
		assertThat(game.status()).as("still in progress, so players can claim").isEqualTo(IN_PROGRESS);
	}

	// --- Claiming ---

	@Test
	void validClaimWinsAndFinishesTheGame() {
		Game game = newGame();
		Player lance = game.join("Lance");
		game.start();
		callUntilBingo(game, lance);

		ClaimResult result = game.claimBingo(lance.id());

		assertThat(result).isInstanceOfSatisfying(ClaimResult.Won.class, won -> {
			assertThat(won.winner()).isEqualTo(lance.id());
			assertThat(won.lines()).isNotEmpty();
		});
		assertThat(game.status()).isEqualTo(FINISHED);
		assertThat(game.winner()).contains(lance.id());
		assertThat(game.snapshot().winner()).contains(lance.id());
	}

	@Test
	void claimWithoutBingoIsRejectedAndTheGameContinues() {
		Game game = newGame();
		Player lance = game.join("Lance");
		game.start();
		game.callNextNumber();

		assertThat(game.claimBingo(lance.id())).isInstanceOf(ClaimResult.NoBingo.class);
		assertThat(game.status()).isEqualTo(IN_PROGRESS);
		assertThat(game.winner()).isEmpty();
	}

	@Test
	void claimAfterSomeoneWonIsTooLate() {
		Game game = newGame();
		Player ann = game.join("Ann");
		Player bob = game.join("Bob");
		game.start();
		callAll(game); // every card is complete now

		assertThat(game.claimBingo(ann.id())).isInstanceOf(ClaimResult.Won.class);
		assertThat(game.claimBingo(bob.id())).isEqualTo(new ClaimResult.GameNotInProgress(FINISHED));
		assertThat(game.winner()).contains(ann.id());
	}

	@Test
	void claimBeforeTheGameStartsIsNotInProgress() {
		Game game = newGame();
		Player lance = game.join("Lance");
		assertThat(game.claimBingo(lance.id())).isEqualTo(new ClaimResult.GameNotInProgress(WAITING_FOR_PLAYERS));
	}

	@Test
	void claimByAPlayerNotInTheGameThrows() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		assertThatIllegalArgumentException().isThrownBy(() -> game.claimBingo(new PlayerId(UUID.randomUUID())));
	}

	@Test
	void cannotCallNumbersAfterTheGameIsWon() {
		Game game = newGame();
		Player lance = game.join("Lance");
		game.start();
		callUntilBingo(game, lance);
		game.claimBingo(lance.id());
		assertThatIllegalStateException().isThrownBy(game::callNextNumber);
	}

	@Test
	void simultaneousClaimsProduceExactlyOneWinner() throws Exception {
		Game game = newGame();
		List<Player> players = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			players.add(game.join("Player " + i));
		}
		game.start();
		callAll(game); // every card is complete, so every claim is valid on its own

		CountDownLatch go = new CountDownLatch(1);
		List<Callable<ClaimResult>> claims = new ArrayList<>();
		for (Player p : players) {
			claims.add(() -> {
				go.await();
				return game.claimBingo(p.id());
			});
		}

		List<ClaimResult> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(players.size())) {
			List<Future<ClaimResult>> futures = new ArrayList<>();
			for (Callable<ClaimResult> claim : claims) {
				futures.add(pool.submit(claim));
			}
			go.countDown();
			for (Future<ClaimResult> f : futures) {
				results.add(f.get());
			}
		}

		assertThat(results).filteredOn(r -> r instanceof ClaimResult.Won).hasSize(1);
		assertThat(results).filteredOn(r -> r instanceof ClaimResult.GameNotInProgress).hasSize(players.size() - 1);
	}

	// --- Snapshot ---

	@Test
	void snapshotDoesNotChangeWhenTheGameDoes() {
		Game game = newGame();
		game.join("Lance");
		game.start();
		game.callNextNumber();
		GameSnapshot before = game.snapshot();

		game.callNextNumber();

		assertThat(before.calledNumbers()).hasSize(1);
		assertThat(before.status()).isEqualTo(IN_PROGRESS);
	}

	@Test
	void sameSeedPlaysTheSameGame() {
		Game a = new Game(GameId.random(), new Random(42));
		Game b = new Game(GameId.random(), new Random(42));
		Player pa = a.join("Lance");
		Player pb = b.join("Lance");
		a.start();
		b.start();
		callAll(a);
		callAll(b);

		assertThat(pa.card()).isEqualTo(pb.card());
		assertThat(a.snapshot().calledNumbers()).isEqualTo(b.snapshot().calledNumbers());
	}
}
