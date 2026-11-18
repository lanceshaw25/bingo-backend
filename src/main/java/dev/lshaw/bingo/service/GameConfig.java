package dev.lshaw.bingo.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Beans the game service needs.
 *
 * <p>{@code @EnableScheduling} makes Spring Boot create the {@code TaskScheduler} that calls numbers.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(BingoProperties.class)
class GameConfig {

	/**
	 * Deals cards and orders calls for every game. {@link SecureRandom} is unpredictable, so a
	 * client cannot work out upcoming numbers, and it is safe to share between threads.
	 */
	@Bean
	RandomGenerator gameRandom() {
		return new SecureRandom();
	}
}
