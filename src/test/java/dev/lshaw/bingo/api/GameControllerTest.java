package dev.lshaw.bingo.api;

import com.jayway.jsonpath.JsonPath;
import dev.lshaw.bingo.game.BingoCard;
import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.Player;
import dev.lshaw.bingo.game.PlayerId;
import dev.lshaw.bingo.game.WinChecker;
import dev.lshaw.bingo.service.BingoProperties;
import dev.lshaw.bingo.service.GameService;
import dev.lshaw.bingo.service.ManualTaskScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP contract: paths, status codes, and JSON shapes. Uses a real {@link GameService}
 * with a {@link ManualTaskScheduler}, so tests decide when numbers are called.
 */
@WebMvcTest(GameController.class)
@Import(GameControllerTest.Config.class)
class GameControllerTest {

	@TestConfiguration
	static class Config {
		@Bean
		ManualTaskScheduler scheduler() {
			return new ManualTaskScheduler();
		}

		@Bean
		GameService gameService(ManualTaskScheduler scheduler) {
			return new GameService(scheduler, new Random(1), new BingoProperties(Duration.ofSeconds(5)));
		}
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private GameService service;

	@Autowired
	private ManualTaskScheduler scheduler;

	private String createGame() throws Exception {
		String body = mvc.perform(post("/api/games")).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String join(String gameId, String name) throws Exception {
		String body = mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"%s\"}".formatted(name)))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.playerId");
	}

	// --- Games ---

	@Test
	void createGameReturnsCreatedWithLocation() throws Exception {
		mvc.perform(post("/api/games"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("http://localhost/api/games/")))
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
				.andExpect(jsonPath("$.players").isEmpty())
				.andExpect(jsonPath("$.calledNumbers").isEmpty())
				.andExpect(jsonPath("$.winner").isEmpty());
	}

	@Test
	void listGamesIncludesCreatedGame() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");

		mvc.perform(get("/api/games"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '%s')].playerCount".formatted(gameId)).value(1))
				.andExpect(jsonPath("$[?(@.id == '%s')].calledCount".formatted(gameId)).value(0));
	}

	@Test
	void getUnknownGameIsNotFoundProblem() throws Exception {
		mvc.perform(get("/api/games/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", "application/problem+json"))
				.andExpect(jsonPath("$.title").value("Game not found"))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void malformedGameIdIsBadRequest() throws Exception {
		mvc.perform(get("/api/games/not-a-uuid")).andExpect(status().isBadRequest());
	}

	// --- Joining and players ---

	@Test
	void joinReturnsPlayerIdAndCard() throws Exception {
		String gameId = createGame();

		mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Lance\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("http://localhost/api/games/" + gameId + "/players/")))
				.andExpect(jsonPath("$.playerId").isString())
				.andExpect(jsonPath("$.name").value("Lance"))
				.andExpect(jsonPath("$.card", hasSize(5)))
				.andExpect(jsonPath("$.card[0]", hasSize(5)))
				.andExpect(jsonPath("$.card[2][2]").value(BingoCard.FREE));
	}

	@Test
	void joinLocationPointsAtThePlayer() throws Exception {
		String gameId = createGame();
		String location = mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Lance\"}"))
				.andReturn().getResponse().getHeader("Location");
		String playerId = location.substring(location.lastIndexOf('/') + 1);

		mvc.perform(get("/api/games/{g}/players/{p}", gameId, playerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Lance"));
	}

	@Test
	void publicGameViewHidesPlayerIdsAndCards() throws Exception {
		String gameId = createGame();
		String playerId = join(gameId, "Lance");

		String body = mvc.perform(get("/api/games/{id}", gameId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.players[0]").value("Lance"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(playerId).doesNotContain("card");
	}

	@Test
	void getPlayerReturnsTheSameCardAsJoin() throws Exception {
		String gameId = createGame();
		String playerId = join(gameId, "Lance");
		Player player = service.player(new GameId(UUID.fromString(gameId)), new PlayerId(UUID.fromString(playerId)));

		mvc.perform(get("/api/games/{g}/players/{p}", gameId, playerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.card[0][0]").value(player.card().numberAt(0, 0)))
				.andExpect(jsonPath("$.card[4][4]").value(player.card().numberAt(4, 4)));
	}

	@Test
	void unknownPlayerIsNotFoundProblem() throws Exception {
		String gameId = createGame();
		mvc.perform(get("/api/games/{g}/players/{p}", gameId, UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Player not found"));
	}

	@Test
	void blankNameIsBadRequest() throws Exception {
		String gameId = createGame();
		mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));
	}

	@Test
	void missingNameIsBadRequest() throws Exception {
		String gameId = createGame();
		mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("name is required"));
	}

	@Test
	void missingBodyIsBadRequest() throws Exception {
		String gameId = createGame();
		mvc.perform(post("/api/games/{id}/players", gameId).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void duplicateNameIsBadRequest() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"LANCE\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", endsWith("has already joined")));
	}

	@Test
	void joiningAfterStartIsConflict() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/start", gameId));

		mvc.perform(post("/api/games/{id}/players", gameId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Late\"}"))
				.andExpect(status().isConflict());
	}

	// --- Starting ---

	@Test
	void startReturnsTheGameInProgress() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");

		mvc.perform(post("/api/games/{id}/start", gameId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
	}

	@Test
	void startingWithNoPlayersIsConflict() throws Exception {
		String gameId = createGame();
		mvc.perform(post("/api/games/{id}/start", gameId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Cannot start a game with no players"));
	}

	@Test
	void calledNumbersAppearInTheGameView() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/start", gameId));
		scheduler.tick(3);

		mvc.perform(get("/api/games/{id}", gameId))
				.andExpect(jsonPath("$.calledNumbers", hasSize(3)));
	}

	// --- Claiming ---

	@Test
	void claimWithoutBingoIsNoBingo() throws Exception {
		String gameId = createGame();
		String playerId = join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/start", gameId));
		scheduler.tick();

		mvc.perform(post("/api/games/{g}/players/{p}/claims", gameId, playerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("NO_BINGO"))
				.andExpect(jsonPath("$.lines").isEmpty());
	}

	@Test
	void winningClaimIsWonAndShowsTheWinner() throws Exception {
		String gameId = createGame();
		String playerId = join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/start", gameId));

		GameId id = new GameId(UUID.fromString(gameId));
		BingoCard card = service.player(id, new PlayerId(UUID.fromString(playerId))).card();
		while (!WinChecker.hasBingo(card, service.snapshot(id).calledNumbers()::contains)) {
			scheduler.tick();
		}

		mvc.perform(post("/api/games/{g}/players/{p}/claims", gameId, playerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("WON"))
				.andExpect(jsonPath("$.lines").isNotEmpty());

		mvc.perform(get("/api/games/{id}", gameId))
				.andExpect(jsonPath("$.status").value("FINISHED"))
				.andExpect(jsonPath("$.winner").value("Lance"));
	}

	@Test
	void claimBeforeStartIsGameNotInProgress() throws Exception {
		String gameId = createGame();
		String playerId = join(gameId, "Lance");

		mvc.perform(post("/api/games/{g}/players/{p}/claims", gameId, playerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("GAME_NOT_IN_PROGRESS"));
	}

	@Test
	void claimByUnknownPlayerIsNotFound() throws Exception {
		String gameId = createGame();
		join(gameId, "Lance");
		mvc.perform(post("/api/games/{id}/start", gameId));

		mvc.perform(post("/api/games/{g}/players/{p}/claims", gameId, UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Player not found"));
	}
}
