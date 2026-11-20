package dev.lshaw.bingo.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.lshaw.bingo.game.Column;
import dev.lshaw.bingo.game.GameEvent;
import dev.lshaw.bingo.game.WinningLine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A game event as published to Kafka, in JSON. This is the public event contract, so it is kept
 * separate from the internal {@link GameEvent} types, just as the REST API has its own response
 * types. In particular, player IDs are secret and never appear here.
 *
 * <p>The {@code type} property names the event:
 * {@code {"type": "NumberCalled", "eventId": "...", "gameId": "...", "sequence": 7, ...}}
 *
 * <p>Common fields:
 * <ul>
 *   <li>{@code eventId}: unique per event, so a consumer can spot a duplicate.
 *   <li>{@code gameId}: also the Kafka message key, so one game's events stay in order.
 *   <li>{@code sequence}: 1, 2, 3, ... per game with no gaps. Matches {@code sequence} in the
 *       REST game view.
 *   <li>{@code occurredAt}: when the server published it (ISO-8601, UTC).
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = GameEventMessage.GameCreated.class, name = "GameCreated"),
		@JsonSubTypes.Type(value = GameEventMessage.PlayerJoined.class, name = "PlayerJoined"),
		@JsonSubTypes.Type(value = GameEventMessage.GameStarted.class, name = "GameStarted"),
		@JsonSubTypes.Type(value = GameEventMessage.NumberCalled.class, name = "NumberCalled"),
		@JsonSubTypes.Type(value = GameEventMessage.GameWon.class, name = "GameWon"),
})
public sealed interface GameEventMessage {

	UUID eventId();

	UUID gameId();

	long sequence();

	Instant occurredAt();

	record GameCreated(UUID eventId, UUID gameId, long sequence, Instant occurredAt) implements GameEventMessage {
	}

	record PlayerJoined(UUID eventId, UUID gameId, long sequence, Instant occurredAt, String name)
			implements GameEventMessage {
	}

	/** @param players names, in the order they joined */
	record GameStarted(UUID eventId, UUID gameId, long sequence, Instant occurredAt, List<String> players)
			implements GameEventMessage {
	}

	/** @param calledCount how many numbers have been called, including this one */
	record NumberCalled(UUID eventId, UUID gameId, long sequence, Instant occurredAt,
			int number, Column column, int calledCount) implements GameEventMessage {
	}

	/** @param winner the winner's name */
	record GameWon(UUID eventId, UUID gameId, long sequence, Instant occurredAt,
			String winner, List<WinningLine> lines) implements GameEventMessage {
	}

	/** Builds the message for an internal event, leaving out anything secret. */
	static GameEventMessage from(GameEvent event, UUID eventId, Instant occurredAt) {
		UUID gameId = event.gameId().value();
		long sequence = event.sequence();
		return switch (event) {
			case GameEvent.GameCreated _ -> new GameCreated(eventId, gameId, sequence, occurredAt);
			case GameEvent.PlayerJoined e -> new PlayerJoined(eventId, gameId, sequence, occurredAt, e.name());
			case GameEvent.GameStarted e -> new GameStarted(eventId, gameId, sequence, occurredAt, e.players());
			case GameEvent.NumberCalled e -> new NumberCalled(eventId, gameId, sequence, occurredAt,
					e.number(), Column.forNumber(e.number()), e.calledCount());
			case GameEvent.GameWon e -> new GameWon(eventId, gameId, sequence, occurredAt, e.winnerName(), e.lines());
		};
	}
}
