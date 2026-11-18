package dev.lshaw.bingo.api;

import dev.lshaw.bingo.game.ClaimResult;
import dev.lshaw.bingo.game.WinningLine;

import java.util.List;

/**
 * The outcome of a Bingo claim.
 *
 * @param lines the winning lines when the result is {@code WON}; otherwise empty
 */
public record ClaimResponse(Result result, List<WinningLine> lines) {

	public enum Result {
		/** The claim was valid and the player won. */
		WON,
		/** The card has no complete line. The game continues. */
		NO_BINGO,
		/** The game had not started, or someone else had already won. */
		GAME_NOT_IN_PROGRESS
	}

	static ClaimResponse from(ClaimResult result) {
		// ClaimResult is sealed, so the compiler checks that every outcome is handled here.
		return switch (result) {
			case ClaimResult.Won won -> new ClaimResponse(Result.WON, won.lines());
			case ClaimResult.NoBingo _ -> new ClaimResponse(Result.NO_BINGO, List.of());
			case ClaimResult.GameNotInProgress _ -> new ClaimResponse(Result.GAME_NOT_IN_PROGRESS, List.of());
		};
	}
}
