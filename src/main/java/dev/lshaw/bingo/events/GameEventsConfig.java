package dev.lshaw.bingo.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** The Kafka topic for game events. */
@Configuration
class GameEventsConfig {

	static final String TOPIC = "bingo.game-events";

	/**
	 * Spring's KafkaAdmin creates this topic at startup if it does not exist. Three partitions let
	 * different games' events be spread out and consumed in parallel; each game stays on one
	 * partition because its ID is the message key. One replica is all a single local broker allows.
	 */
	@Bean
	NewTopic gameEventsTopic() {
		return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
	}
}
