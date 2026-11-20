package dev.lshaw.bingo.events;

import dev.lshaw.bingo.game.GameEvent;
import dev.lshaw.bingo.game.GameEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes every game event to the {@value GameEventsConfig#TOPIC} topic, keyed by game ID.
 *
 * <p>Kafka sends all messages with the same key to the same partition and keeps each partition in
 * order, so one game's events are consumed in the order they happened.
 *
 * <p>Sending is asynchronous. The Kafka producer retries failures on its own (for up to
 * {@code delivery.timeout.ms}, two minutes by default). If it still fails, the event is logged
 * and lost; the game carries on. The in-memory game is the source of truth, and a client that
 * notices a gap in {@code sequence} can fetch the game's current state over REST.
 */
@Component
class KafkaGameEventPublisher implements GameEventListener {

	private static final Logger log = LoggerFactory.getLogger(KafkaGameEventPublisher.class);

	private final KafkaTemplate<String, GameEventMessage> kafka;

	KafkaGameEventPublisher(KafkaTemplate<String, GameEventMessage> kafka) {
		this.kafka = kafka;
	}

	@Override
	public void onEvent(GameEvent event) {
		GameEventMessage message = GameEventMessage.from(event, UUID.randomUUID(), Instant.now());
		String key = event.gameId().toString();
		try {
			kafka.send(GameEventsConfig.TOPIC, key, message).whenComplete((result, failure) -> {
				if (failure != null) {
					log.error("Failed to publish {} #{} for game {}", type(message), message.sequence(), key, failure);
				} else {
					log.debug("Published {} #{} for game {} to partition {}", type(message), message.sequence(), key,
							result.getRecordMetadata().partition());
				}
			});
		} catch (RuntimeException e) {
			// send() can also fail before it returns, such as when no broker is reachable within
			// max.block.ms. The game's state has already changed, so log it rather than throw.
			log.error("Failed to publish {} #{} for game {}", type(message), message.sequence(), key, e);
		}
	}

	private static String type(GameEventMessage message) {
		return message.getClass().getSimpleName();
	}
}
