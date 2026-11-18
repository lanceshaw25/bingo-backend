package dev.lshaw.bingo.api;

import dev.lshaw.bingo.game.BingoCard;
import dev.lshaw.bingo.game.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player's private view: their ID and card. Only returned to the player themselves.
 *
 * @param card five rows of five numbers, columns B-I-N-G-O left to right, with {@code 0} for the
 *     free center square
 */
public record PlayerView(UUID playerId, String name, List<List<Integer>> card) {

	static PlayerView from(Player player) {
		return new PlayerView(player.id().value(), player.name(), rows(player.card()));
	}

	private static List<List<Integer>> rows(BingoCard card) {
		List<List<Integer>> rows = new ArrayList<>();
		for (int row = 0; row < BingoCard.SIZE; row++) {
			List<Integer> numbers = new ArrayList<>();
			for (int col = 0; col < BingoCard.SIZE; col++) {
				numbers.add(card.numberAt(row, col));
			}
			rows.add(List.copyOf(numbers));
		}
		return List.copyOf(rows);
	}
}
