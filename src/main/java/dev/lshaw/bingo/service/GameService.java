package dev.lshaw.bingo.service;

import dev.lshaw.bingo.game.ClaimResult;
import dev.lshaw.bingo.game.Column;
import dev.lshaw.bingo.game.Game;
import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.GameSnapshot;
import dev.lshaw.bingo.game.Player;
import dev.lshaw.bingo.game.PlayerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.random.RandomGenerator;

/**
 * Holds every game and runs the timer that calls each game's numbers.
 *
 * <p>Games are kept in memory, so they are lost when the server restarts. Game rules live in
 * {@link Game}; this class finds the right game, delegates to it, and schedules number calls.
 */
@Service
public class GameService {

	private static final Logger log = LoggerFactory.getLogger(GameService.class);

	private final Map<GameId, Game> games = new ConcurrentHashMap<>();
	/** The repeating number-calling task for each game that is being called. */
	private final Map<GameId, ScheduledFuture<?>> callingTasks = new ConcurrentHashMap<>();

	private final TaskScheduler scheduler;
	private final RandomGenerator rng;
	private final Duration callInterval;

	public GameService(TaskScheduler scheduler, RandomGenerator rng, BingoProperties properties) {
		this.scheduler = scheduler;
		this.rng = rng;
		this.callInterval = properties.callInterval();
	}

	/** Creates a game that is waiting for players. */
	public GameSnapshot createGame() {
		Game game = new Game(GameId.random(), rng);
		games.put(game.id(), game);
		log.info("Created game {}", game.id());
		return game.snapshot();
	}

	/** Every game, in no particular order. */
	public List<GameSnapshot> games() {
		return games.values().stream().map(Game::snapshot).toList();
	}

	/** @throws GameNotFoundException if there is no such game */
	public GameSnapshot snapshot(GameId gameId) {
		return find(gameId).snapshot();
	}

	/**
	 * @throws GameNotFoundException if there is no such game
	 * @see Game#join(String)
	 */
	public Player join(GameId gameId, String name) {
		Player player = find(gameId).join(name);
		log.info("Player {} ({}) joined game {}", player.name(), player.id(), gameId);
		return player;
	}

	/**
	 * @throws GameNotFoundException if there is no such game
	 * @throws PlayerNotFoundException if the game has no such player
	 */
	public Player player(GameId gameId, PlayerId playerId) {
		return find(gameId).player(playerId).orElseThrow(() -> new PlayerNotFoundException(gameId, playerId));
	}

	/**
	 * Starts the game and begins calling numbers, the first one {@code bingo.call-interval} from now.
	 *
	 * @throws GameNotFoundException if there is no such game
	 * @see Game#start()
	 */
	public void start(GameId gameId) {
		find(gameId).start();
		Instant firstCall = scheduler.getClock().instant().plus(callInterval);
		callingTasks.put(gameId, scheduler.scheduleWithFixedDelay(() -> callNextNumber(gameId), firstCall, callInterval));
		log.info("Started game {}; calling a number every {}", gameId, callInterval);
	}

	/**
	 * A player claims Bingo. A winning claim also stops the game's number calling.
	 *
	 * @throws GameNotFoundException if there is no such game
	 * @throws PlayerNotFoundException if the game has no such player
	 * @see Game#claimBingo(PlayerId)
	 */
	public ClaimResult claimBingo(GameId gameId, PlayerId playerId) {
		Game game = find(gameId);
		// Players are never removed, so this check cannot go stale before the claim below.
		if (game.player(playerId).isEmpty()) {
			throw new PlayerNotFoundException(gameId, playerId);
		}
		ClaimResult result = game.claimBingo(playerId);
		if (result instanceof ClaimResult.Won won) {
			stopCalling(gameId);
			log.info("Player {} won game {} with {}", playerId, gameId, won.lines());
		}
		return result;
	}

	/** Run by the scheduler for each game. Stops itself when the game has nothing left to call. */
	private void callNextNumber(GameId gameId) {
		Game game = games.get(gameId);
		OptionalInt number = game == null ? OptionalInt.empty() : game.tryCallNextNumber();
		if (number.isPresent()) {
			int n = number.getAsInt();
			log.info("Game {} called {}-{}", gameId, Column.forNumber(n), n);
		} else {
			stopCalling(gameId);
		}
	}

	private void stopCalling(GameId gameId) {
		ScheduledFuture<?> task = callingTasks.remove(gameId);
		if (task != null) {
			task.cancel(false);
			log.info("Stopped calling numbers for game {}", gameId);
		}
	}

	private Game find(GameId gameId) {
		Game game = games.get(gameId);
		if (game == null) {
			throw new GameNotFoundException(gameId);
		}
		return game;
	}
}
