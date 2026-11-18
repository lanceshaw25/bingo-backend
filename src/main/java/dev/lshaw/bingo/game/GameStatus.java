package dev.lshaw.bingo.game;

/**
 * Where a game is in its lifecycle. A game only moves forward:
 * {@code WAITING_FOR_PLAYERS -> IN_PROGRESS -> FINISHED}.
 */
public enum GameStatus {
	/** Players can join. No numbers have been called. */
	WAITING_FOR_PLAYERS,
	/** Numbers are being called and players can claim Bingo. No one can join. */
	IN_PROGRESS,
	/** A player has won. */
	FINISHED
}
