package dev.lshaw.bingo.events;

import com.jayway.jsonpath.JsonPath;
import dev.lshaw.bingo.game.Column;
import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.Player;
import dev.lshaw.bingo.game.WinChecker;
import dev.lshaw.bingo.service.GameService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Plays real games against an embedded Kafka broker and reads the raw JSON from the topic, the
 * way any other consumer would.
 */
@SpringBootTest(properties = "bingo.call-interval=20ms")
@EmbeddedKafka
class GameEventsKafkaTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Autowired
	private GameService games;

	@Autowired
	private EmbeddedKafkaBroker broker;

	private Consumer<String, String> consumer;
	/** Every record this test has read, from any game. */
	private final List<ConsumerRecord<String, String>> received = new ArrayList<>();

	@BeforeEach
	void subscribe() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "test-" + UUID.randomUUID(), false);
		props.put("auto.offset.reset", "earliest");
		consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
		consumer.subscribe(List.of(GameEventsConfig.TOPIC));
	}

	@AfterEach
	void close() {
		consumer.close();
	}

	/** Reads from the topic until this game's events satisfy the condition. */
	private List<ConsumerRecord<String, String>> eventsFor(GameId game, Predicate<List<String>> until) {
		Instant deadline = Instant.now().plus(TIMEOUT);
		while (Instant.now().isBefore(deadline)) {
			consumer.poll(Duration.ofMillis(100)).forEach(received::add);
			List<ConsumerRecord<String, String>> mine =
					received.stream().filter(r -> r.key().equals(game.toString())).toList();
			if (until.test(mine.stream().map(ConsumerRecord::value).toList())) {
				return mine;
			}
		}
		return fail("Timed out waiting for events for game " + game);
	}

	private static String type(String json) {
		return JsonPath.read(json, "$.type");
	}

	private static long countOf(List<String> json, String type) {
		return json.stream().filter(j -> type(j).equals(type)).count();
	}

	@Test
	void topicHasThreePartitions() {
		assertThat(consumer.partitionsFor(GameEventsConfig.TOPIC)).hasSize(3);
	}

	@Test
	void gameEventsArriveInOrderOnOnePartitionKeyedByGame() {
		GameId game = games.createGame().id();
		Player lance = games.join(game, "Lance");
		games.start(game);

		List<ConsumerRecord<String, String>> records = eventsFor(game, json -> countOf(json, "NumberCalled") >= 3);
		List<String> json = records.stream().map(ConsumerRecord::value).toList();

		assertThat(records).extracting(ConsumerRecord::partition).containsOnly(records.getFirst().partition());
		assertThat(json.subList(0, 6)).extracting(GameEventsKafkaTest::type).containsExactly(
				"GameCreated", "PlayerJoined", "GameStarted", "NumberCalled", "NumberCalled", "NumberCalled");
		for (int i = 0; i < json.size(); i++) {
			String event = json.get(i);
			assertThat(JsonPath.<Integer>read(event, "$.sequence")).isEqualTo(i + 1);
			assertThat(JsonPath.<String>read(event, "$.gameId")).isEqualTo(game.toString());
			assertThat(JsonPath.<String>read(event, "$.eventId")).isNotBlank();
			assertThat(Instant.parse(JsonPath.read(event, "$.occurredAt"))).isBeforeOrEqualTo(Instant.now());
			// The player ID is the player's secret; it must never go out on the topic.
			assertThat(event).doesNotContain(lance.id().toString());
		}
		assertThat(JsonPath.<String>read(json.get(1), "$.name")).isEqualTo("Lance");
		assertThat(JsonPath.<List<String>>read(json.get(2), "$.players")).containsExactly("Lance");
		String called = json.get(3);
		assertThat(JsonPath.<Integer>read(called, "$.calledCount")).isEqualTo(1);
		int number = JsonPath.read(called, "$.number");
		assertThat(games.snapshot(game).calledNumbers().getFirst()).isEqualTo(number);
		assertThat(JsonPath.<String>read(called, "$.column")).isEqualTo(Column.forNumber(number).name());
		// No Spring type header: consumers go by the JSON "type" field, not our class names.
		assertThat(records.getFirst().headers().lastHeader("__TypeId__")).isNull();
	}

	@Test
	void winningPublishesGameWonWithTheWinnersName() {
		GameId game = games.createGame().id();
		Player lance = games.join(game, "Lance");
		games.start(game);
		Instant deadline = Instant.now().plus(TIMEOUT);
		while (!WinChecker.hasBingo(lance.card(), games.snapshot(game).calledNumbers()::contains)) {
			assertThat(Instant.now()).as("waiting for Bingo").isBefore(deadline);
			Thread.onSpinWait();
		}
		games.claimBingo(game, lance.id());

		List<String> json = eventsFor(game, events -> countOf(events, "GameWon") == 1).stream()
				.map(ConsumerRecord::value).toList();
		String won = json.getLast();
		assertThat(type(won)).isEqualTo("GameWon");
		assertThat(JsonPath.<String>read(won, "$.winner")).isEqualTo("Lance");
		assertThat(JsonPath.<List<String>>read(won, "$.lines")).isNotEmpty();
		assertThat(JsonPath.<Integer>read(won, "$.sequence")).isEqualTo(games.snapshot(game).sequence());
		assertThat(won).doesNotContain(lance.id().toString());
	}
}
