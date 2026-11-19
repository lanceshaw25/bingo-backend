package dev.lshaw.bingo.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title and description for the generated OpenAPI spec. springdoc builds the rest (endpoints,
 * request and response shapes, error responses) from the controllers, {@link ApiExceptionHandler},
 * and the response records.
 *
 * <p>Spec: {@code /v3/api-docs}. Swagger UI: {@code /swagger-ui.html}.
 */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI bingoOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Real-Time Bingo API")
				.version("v1")
				.description("""
						Create, join, and play Bingo games. See API.md in the bingo-backend \
						repository for the concepts behind the API.

						**Player IDs are secret.** Joining returns a `playerId`, the only thing \
						that lets a client fetch that player's card or claim Bingo for them. \
						The public game view lists players by name only."""));
	}
}
