package dev.lshaw.bingo.api;

import dev.lshaw.bingo.game.GameSnapshot;
import dev.lshaw.bingo.game.GameStatus;
import dev.lshaw.bingo.game.Player;

import java.util.List;
import java.util.UUID;

/**
 * The public state of a game, visible to anyone.
 *
 * <p>Players are listed by name only. Player IDs and cards are left out on purpose: a player ID
 * is what lets someone fetch a card or claim Bingo, so it must only go to that player.
 *
 * @param winner the winning player's name, or {@code null} if no one has won
 * @param sequence the sequence number of the last game event this view includes. A client that
 *     also receives live events can ignore any event at or below it.
 */
public record GameView(
		UUID id,
		GameStatus status,
		List<String> players,
		List<Integer> calledNumbers,
		String winner,
		long sequence) {

	static GameView from(GameSnapshot snapshot) {
		String winner = snapshot.winner()
				.flatMap(winnerId -> snapshot.players().stream().filter(p -> p.id().equals(winnerId)).findFirst())
				.map(Player::name)
				.orElse(null);
		return new GameView(
				snapshot.id().value(),
				snapshot.status(),
				snapshot.players().stream().map(Player::name).toList(),
				snapshot.calledNumbers(),
				winner,
				snapshot.sequence());
	}
}
