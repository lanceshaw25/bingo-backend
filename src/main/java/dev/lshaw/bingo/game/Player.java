package dev.lshaw.bingo.game;

import java.util.Objects;

/** A player in a game and the card they were dealt. */
public record Player(PlayerId id, String name, BingoCard card) {

	public Player {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(card, "card");
		if (name.isBlank()) {
			throw new IllegalArgumentException("Player name must not be blank");
		}
	}
}
