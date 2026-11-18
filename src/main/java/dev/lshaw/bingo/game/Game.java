package dev.lshaw.bingo.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static dev.lshaw.bingo.game.GameStatus.FINISHED;
import static dev.lshaw.bingo.game.GameStatus.IN_PROGRESS;
import static dev.lshaw.bingo.game.GameStatus.WAITING_FOR_PLAYERS;

/**
 * One game of Bingo: its players, their cards, the numbers called, and who won.
 *
 * <p>Rules:
 * <ul>
 *   <li>Players join before the game starts. Names must be unique, ignoring case.
 *   <li>A game needs at least one player to start.
 *   <li>The first valid claim wins and finishes the game. Later claims get
 *       {@link ClaimResult.GameNotInProgress}.
 *   <li>An invalid claim has no penalty; the player stays in the game.
 *   <li>After all 75 numbers are called, the game stays in progress so players can still claim.
 * </ul>
 *
 * <p>The game does not decide when to call numbers. Something outside it, such as a scheduler,
 * calls {@link #callNextNumber()}.
 *
 * <p>Thread-safe: every public method that reads or changes state holds the game's lock, so
 * requests and the number-calling scheduler can use the same game from different threads.
 */
public final class Game {

	private final GameId id;
	private final RandomGenerator rng;
	private final NumberCaller caller;
	private final Map<PlayerId, Player> players = new LinkedHashMap<>();

	private GameStatus status = WAITING_FOR_PLAYERS;
	private PlayerId winner;

	/**
	 * Creates a game waiting for players. The generator deals cards and orders the calls; pass a
	 * seeded one to get the same game every time.
	 */
	public Game(GameId id, RandomGenerator rng) {
		this.id = Objects.requireNonNull(id, "id");
		this.rng = Objects.requireNonNull(rng, "rng");
		this.caller = new NumberCaller(rng);
	}

	public GameId id() {
		return id;
	}

	/**
	 * Adds a player and deals them a card.
	 *
	 * @param name the player's display name; leading and trailing spaces are removed
	 * @throws IllegalStateException if the game has already started
	 * @throws IllegalArgumentException if the name is blank or another player already has it
	 */
	public synchronized Player join(String name) {
		Objects.requireNonNull(name, "name");
		requireStatus(WAITING_FOR_PLAYERS, "join");

		String trimmed = name.strip();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Player name must not be blank");
		}
		for (Player existing : players.values()) {
			if (existing.name().equalsIgnoreCase(trimmed)) {
				throw new IllegalArgumentException("A player named '%s' has already joined".formatted(existing.name()));
			}
		}

		Player player = new Player(PlayerId.random(), trimmed, BingoCard.random(rng));
		players.put(player.id(), player);
		return player;
	}

	/**
	 * Starts the game. No one can join after this.
	 *
	 * @throws IllegalStateException if the game has already started or has no players
	 */
	public synchronized void start() {
		requireStatus(WAITING_FOR_PLAYERS, "start");
		if (players.isEmpty()) {
			throw new IllegalStateException("Cannot start a game with no players");
		}
		status = IN_PROGRESS;
	}

	/**
	 * Calls the next number.
	 *
	 * @throws IllegalStateException if the game is not in progress or all 75 numbers have been called
	 */
	public synchronized int callNextNumber() {
		requireStatus(IN_PROGRESS, "call a number");
		return caller.callNext();
	}

	/** Whether there are numbers left to call. A scheduler can use this to know when to stop. */
	public synchronized boolean hasNumbersLeft() {
		return caller.hasNext();
	}

	/**
	 * A player claims Bingo. The claim is checked against the player's card and the numbers called
	 * so far. A valid claim wins and finishes the game.
	 *
	 * @throws IllegalArgumentException if the player is not in this game
	 */
	public synchronized ClaimResult claimBingo(PlayerId playerId) {
		Objects.requireNonNull(playerId, "playerId");
		Player player = players.get(playerId);
		if (player == null) {
			throw new IllegalArgumentException("Player %s is not in game %s".formatted(playerId, id));
		}
		if (status != IN_PROGRESS) {
			return new ClaimResult.GameNotInProgress(status);
		}

		List<WinningLine> lines = WinChecker.winningLines(player.card(), caller::isCalled);
		if (lines.isEmpty()) {
			return new ClaimResult.NoBingo();
		}
		winner = playerId;
		status = FINISHED;
		return new ClaimResult.Won(playerId, lines);
	}

	public synchronized GameStatus status() {
		return status;
	}

	public synchronized Optional<Player> player(PlayerId playerId) {
		return Optional.ofNullable(players.get(playerId));
	}

	public synchronized Optional<PlayerId> winner() {
		return Optional.ofNullable(winner);
	}

	/** A consistent copy of the game's state. Later changes to the game do not affect it. */
	public synchronized GameSnapshot snapshot() {
		return new GameSnapshot(id, status, List.copyOf(players.values()), caller.calledNumbers(),
				Optional.ofNullable(winner));
	}

	private void requireStatus(GameStatus required, String action) {
		if (status != required) {
			throw new IllegalStateException("Cannot %s: game %s is %s, not %s".formatted(action, id, status, required));
		}
	}
}
