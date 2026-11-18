package dev.lshaw.bingo.game;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of a player claiming Bingo. Sealed, so a {@code switch} over a result must handle
 * every outcome.
 */
public sealed interface ClaimResult {

	/** The claim was valid and the player won the game. */
	record Won(PlayerId winner, List<WinningLine> lines) implements ClaimResult {
		public Won {
			Objects.requireNonNull(winner, "winner");
			lines = List.copyOf(lines);
		}
	}

	/** The player's card does not have a complete line. The game continues. */
	record NoBingo() implements ClaimResult {
	}

	/**
	 * The game was not accepting claims: it had not started, or someone had already won. A player
	 * who claims just after another player wins gets this result.
	 */
	record GameNotInProgress(GameStatus status) implements ClaimResult {
		public GameNotInProgress {
			Objects.requireNonNull(status, "status");
		}
	}
}
