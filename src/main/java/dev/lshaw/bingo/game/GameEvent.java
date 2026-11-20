package dev.lshaw.bingo.game;

import java.util.List;
import java.util.Objects;

/**
 * Something that happened in a game. A {@link Game} creates these as its state changes and hands
 * them to its {@link GameEventListener}.
 *
 * <p>Every event has a {@link #sequence()}: 1 for the first event in a game, then 2, 3, and so on,
 * with no gaps. A {@link GameSnapshot} carries the sequence of the last event it includes, so a
 * client that gets a snapshot can tell which events it already has.
 *
 * <p>These are internal: they may carry player IDs, which must never reach other clients. Code
 * that publishes events decides what goes out.
 */
public sealed interface GameEvent {

	GameId gameId();

	long sequence();

	record GameCreated(GameId gameId, long sequence) implements GameEvent {
		public GameCreated {
			Objects.requireNonNull(gameId, "gameId");
		}
	}

	record PlayerJoined(GameId gameId, long sequence, PlayerId playerId, String name) implements GameEvent {
		public PlayerJoined {
			Objects.requireNonNull(gameId, "gameId");
			Objects.requireNonNull(playerId, "playerId");
			Objects.requireNonNull(name, "name");
		}
	}

	/** @param players the players' names, in the order they joined */
	record GameStarted(GameId gameId, long sequence, List<String> players) implements GameEvent {
		public GameStarted {
			Objects.requireNonNull(gameId, "gameId");
			players = List.copyOf(players);
		}
	}

	/** @param calledCount how many numbers have been called, including this one */
	record NumberCalled(GameId gameId, long sequence, int number, int calledCount) implements GameEvent {
		public NumberCalled {
			Objects.requireNonNull(gameId, "gameId");
		}
	}

	record GameWon(GameId gameId, long sequence, PlayerId winnerId, String winnerName, List<WinningLine> lines)
			implements GameEvent {
		public GameWon {
			Objects.requireNonNull(gameId, "gameId");
			Objects.requireNonNull(winnerId, "winnerId");
			Objects.requireNonNull(winnerName, "winnerName");
			lines = List.copyOf(lines);
		}
	}
}
