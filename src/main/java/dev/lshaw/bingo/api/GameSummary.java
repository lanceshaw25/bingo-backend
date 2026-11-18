package dev.lshaw.bingo.api;

import dev.lshaw.bingo.game.GameSnapshot;
import dev.lshaw.bingo.game.GameStatus;

import java.util.UUID;

/** A game in the list of games: enough to choose one to join. */
public record GameSummary(UUID id, GameStatus status, int playerCount, int calledCount) {

	static GameSummary from(GameSnapshot snapshot) {
		return new GameSummary(
				snapshot.id().value(),
				snapshot.status(),
				snapshot.players().size(),
				snapshot.calledNumbers().size());
	}
}
