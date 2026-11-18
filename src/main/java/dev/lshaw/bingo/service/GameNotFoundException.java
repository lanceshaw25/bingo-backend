package dev.lshaw.bingo.service;

import dev.lshaw.bingo.game.GameId;

/** No game exists with the given ID. */
public class GameNotFoundException extends RuntimeException {

	private final GameId gameId;

	public GameNotFoundException(GameId gameId) {
		super("No game with ID " + gameId);
		this.gameId = gameId;
	}

	public GameId gameId() {
		return gameId;
	}
}
