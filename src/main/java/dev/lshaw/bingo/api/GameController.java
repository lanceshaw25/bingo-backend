package dev.lshaw.bingo.api;

import dev.lshaw.bingo.game.GameId;
import dev.lshaw.bingo.game.GameSnapshot;
import dev.lshaw.bingo.game.Player;
import dev.lshaw.bingo.game.PlayerId;
import dev.lshaw.bingo.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * The REST API for games. This is the public contract shared by every client (React, lanceBot), so
 * it returns its own response types rather than domain classes. See {@code API.md}.
 *
 * <p>Errors are turned into HTTP responses by {@link ApiExceptionHandler}.
 */
@Tag(name = "Games")
@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameService games;

	public GameController(GameService games) {
		this.games = games;
	}

	@Operation(summary = "Create a game")
	@ApiResponse(responseCode = "201", description = "Created. `Location` is the new game.")
	@PostMapping
	public ResponseEntity<GameView> createGame() {
		GameSnapshot game = games.createGame();
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{gameId}").buildAndExpand(game.id().value()).toUri();
		return ResponseEntity.created(location).body(GameView.from(game));
	}

	@Operation(summary = "List games")
	@GetMapping
	public List<GameSummary> listGames() {
		return games.games().stream().map(GameSummary::from).toList();
	}

	@Operation(summary = "Get a game", description = "The public game state. Lists players by name only.")
	@GetMapping("/{gameId}")
	public GameView getGame(@PathVariable UUID gameId) {
		return GameView.from(games.snapshot(new GameId(gameId)));
	}

	@Operation(summary = "Join a game", description = """
			Returns the new player's secret `playerId` and card. Names are trimmed and must be \
			unique in the game, ignoring case. Only allowed before the game starts.""")
	@ApiResponse(responseCode = "201", description = "Joined. `Location` is the new player.")
	@PostMapping("/{gameId}/players")
	public ResponseEntity<PlayerView> join(@PathVariable UUID gameId, @RequestBody JoinRequest request) {
		if (request.name() == null) {
			throw new IllegalArgumentException("name is required");
		}
		Player player = games.join(new GameId(gameId), request.name());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{playerId}").buildAndExpand(player.id().value()).toUri();
		return ResponseEntity.created(location).body(PlayerView.from(player));
	}

	@Operation(summary = "Get your player", description = "Your card, for example after reconnecting.")
	@GetMapping("/{gameId}/players/{playerId}")
	public PlayerView getPlayer(@PathVariable UUID gameId, @PathVariable UUID playerId) {
		return PlayerView.from(games.player(new GameId(gameId), new PlayerId(playerId)));
	}

	@Operation(summary = "Start a game", description = """
			Needs at least one player. The server then calls a number every \
			`bingo.call-interval`.""")
	@PostMapping("/{gameId}/start")
	public GameView start(@PathVariable UUID gameId) {
		GameId id = new GameId(gameId);
		games.start(id);
		return GameView.from(games.snapshot(id));
	}

	@Operation(summary = "Claim Bingo", description = """
			Checked against your card and the numbers the server has called. `NO_BINGO` has \
			no penalty.""")
	@PostMapping("/{gameId}/players/{playerId}/claims")
	public ClaimResponse claimBingo(@PathVariable UUID gameId, @PathVariable UUID playerId) {
		return ClaimResponse.from(games.claimBingo(new GameId(gameId), new PlayerId(playerId)));
	}
}
