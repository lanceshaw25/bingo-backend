package dev.lshaw.bingo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
// The app creates its Kafka topic at startup; without a broker it would wait for a timeout.
@EmbeddedKafka
class BingoApplicationTests {

	@Test
	void contextLoads() {
	}

}
