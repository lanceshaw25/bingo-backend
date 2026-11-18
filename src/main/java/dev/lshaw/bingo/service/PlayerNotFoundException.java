package dev.lshaw.bingo.service;

import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.PlayerId;

/** The game exists, but has no player with the given ID. */
public class PlayerNotFoundException extends RuntimeException {

	private final GameId gameId;
	private final PlayerId playerId;

	public PlayerNotFoundException(GameId gameId, PlayerId playerId) {
		super("No player with ID %s in game %s".formatted(playerId, gameId));
		this.gameId = gameId;
		this.playerId = playerId;
	}

	public GameId gameId() {
		return gameId;
	}

	public PlayerId playerId() {
		return playerId;
	}
}
