package dev.lshaw.bingo.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TaskScheduler} for tests that never runs anything on its own. It records scheduled
 * tasks, and {@link #tick()} runs each active one once, as if its delay had passed. Tests stay
 * fast and repeatable because nothing waits on a real clock.
 *
 * <p>Only {@link #scheduleWithFixedDelay(Runnable, Instant, Duration)} is supported, since that is
 * all {@link GameService} uses.
 */
class ManualTaskScheduler implements TaskScheduler {

	static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	record ScheduledTask(Runnable task, Instant startTime, Duration delay, ManualFuture future) {
		boolean isActive() {
			return !future.isCancelled();
		}
	}

	private final List<ScheduledTask> tasks = new ArrayList<>();

	List<ScheduledTask> tasks() {
		return List.copyOf(tasks);
	}

	List<ScheduledTask> activeTasks() {
		return tasks.stream().filter(ScheduledTask::isActive).toList();
	}

	/** Runs every task that has not been cancelled, once. */
	void tick() {
		for (ScheduledTask t : activeTasks()) {
			t.task().run();
		}
	}

	void tick(int times) {
		for (int i = 0; i < times; i++) {
			tick();
		}
	}

	@Override
	public Clock getClock() {
		return Clock.fixed(NOW, ZoneOffset.UTC);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		ManualFuture future = new ManualFuture();
		tasks.add(new ScheduledTask(task, startTime, delay, future));
		return future;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		throw new UnsupportedOperationException();
	}

	/** A future that only tracks whether it was cancelled. */
	static final class ManualFuture implements ScheduledFuture<Object> {

		private volatile boolean cancelled;

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return cancelled;
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(long timeout, TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed other) {
			return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
		}
	}
}
