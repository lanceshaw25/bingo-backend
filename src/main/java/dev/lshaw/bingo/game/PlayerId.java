package dev.lshaw.bingo.game;

import java.util.Objects;
import java.util.UUID;

/** Identifies a player within a game. */
public record PlayerId(UUID value) {

	public PlayerId {
		Objects.requireNonNull(value, "value");
	}

	public static PlayerId random() {
		return new PlayerId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
