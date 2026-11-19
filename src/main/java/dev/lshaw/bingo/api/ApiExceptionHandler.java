package dev.lshaw.bingo.api;

import dev.lshaw.bingo.service.GameNotFoundException;
import dev.lshaw.bingo.service.PlayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions from the game code into HTTP error responses, as {@link ProblemDetail} JSON
 * (RFC 9457): {@code {"status": 404, "title": "Game not found", "detail": "...", ...}}.
 *
 * <p>Game rules throw {@link IllegalArgumentException} for bad input and
 * {@link IllegalStateException} for an action the game's status does not allow. Mapping those
 * standard exceptions here is simple, but it also catches any other code that throws them. If
 * that causes confusing responses, the game code should throw its own exception types instead.
 *
 * <p>The {@link ResponseStatus} annotations repeat each status so springdoc can list these error
 * responses in the OpenAPI spec. It cannot see the status set inside the {@link ProblemDetail}.
 */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(GameNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ProblemDetail gameNotFound(GameNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "Game not found", e);
	}

	@ExceptionHandler(PlayerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ProblemDetail playerNotFound(PlayerNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "Player not found", e);
	}

	/** Bad input, such as a blank or duplicate player name. */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ProblemDetail badRequest(IllegalArgumentException e) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", e);
	}

	/** An action the game's current status does not allow, such as joining after it started. */
	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ProblemDetail conflict(IllegalStateException e) {
		return problem(HttpStatus.CONFLICT, "Not allowed in the game's current state", e);
	}

	private static ProblemDetail problem(HttpStatus status, String title, Exception e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
		problem.setTitle(title);
		return problem;
	}
}
