package dev.lshaw.bingo.game;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A consistent, immutable copy of a game's state at one moment.
 *
 * <p>This is the server's full view, including every player's card. The API layer decides what
 * each client is allowed to see.
 *
 * @param sequence the sequence number of the last {@link GameEvent} included in this snapshot.
 *     A client that also receives events can ignore any event at or below it.
 */
public record GameSnapshot(
		GameId id,
		GameStatus status,
		List<Player> players,
		List<Integer> calledNumbers,
		Optional<PlayerId> winner,
		long sequence) {

	public GameSnapshot {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(winner, "winner");
		players = List.copyOf(players);
		calledNumbers = List.copyOf(calledNumbers);
	}
}
