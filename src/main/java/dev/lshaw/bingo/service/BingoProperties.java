package dev.lshaw.bingo.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

/**
 * Settings under {@code bingo.*} in {@code application.yaml}.
 *
 * @param callInterval how long to wait between called numbers ({@code bingo.call-interval})
 */
@ConfigurationProperties("bingo")
public record BingoProperties(@DefaultValue("5s") Duration callInterval) {

	public BingoProperties {
		Objects.requireNonNull(callInterval, "callInterval");
		if (callInterval.isZero() || callInterval.isNegative()) {
			throw new IllegalArgumentException("bingo.call-interval must be positive, but was " + callInterval);
		}
	}
}
