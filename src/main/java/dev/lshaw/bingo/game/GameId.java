package dev.lshaw.bingo.game;

import java.util.Objects;
import java.util.UUID;

/** Identifies a game. */
public record GameId(UUID value) {

	public GameId {
		Objects.requireNonNull(value, "value");
	}

	public static GameId random() {
		return new GameId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
