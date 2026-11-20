package dev.lshaw.bingo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The generated OpenAPI spec is served and describes the game endpoints. */
@SpringBootTest
// The app creates its Kafka topic at startup; without a broker it would wait for a timeout.
@EmbeddedKafka
@AutoConfigureMockMvc
class OpenApiDocsTest {

	@Autowired
	MockMvc mvc;

	@Test
	void specDescribesTheGameEndpoints() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Real-Time Bingo API"))
				.andExpect(jsonPath("$.paths", hasKey("/api/games")))
				.andExpect(jsonPath("$.paths", hasKey("/api/games/{gameId}/players/{playerId}/claims")))
				.andExpect(jsonPath("$.paths['/api/games/{gameId}'].get.responses", hasKey("404")))
				.andExpect(jsonPath("$.paths['/api/games'].post.responses", hasKey("201")))
				.andExpect(jsonPath("$.paths['/api/games'].post.responses", not(hasKey("200"))));
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}
}
