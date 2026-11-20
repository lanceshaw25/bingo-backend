package dev.lshaw.bingo.game;

/**
 * Receives a game's events as they happen.
 *
 * <p>The game calls {@link #onEvent} while holding its lock, right after the state change, so
 * events arrive in sequence order and match the game's state. Because the lock is held:
 * <ul>
 *   <li>Return quickly. Hand the event off (for example, to Kafka's asynchronous send) rather
 *       than doing slow work.
 *   <li>Do not throw. The state has already changed; an exception would reach the caller as if
 *       the action had failed.
 *   <li>Do not call back into the game from another thread and wait for it.
 * </ul>
 */
@FunctionalInterface
public interface GameEventListener {

	/** A listener that ignores every event. */
	GameEventListener NONE = event -> {
	};

	void onEvent(GameEvent event);
}
