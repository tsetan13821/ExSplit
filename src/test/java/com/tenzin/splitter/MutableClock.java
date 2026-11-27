package com.tenzin.splitter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real time plus an offset that tests can push forward, so expiry and lockout can be
 * tested without sleeping. Tests that advance it must call {@link #reset()} afterwards,
 * because the Spring context (and this clock) is shared between test classes.
 */
public class MutableClock extends Clock {

	private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

	public void advance(Duration amount) {
		offset.updateAndGet(current -> current.plus(amount));
	}

	/** Jumps to a given moment; time keeps ticking from there. */
	public void travelTo(Instant target) {
		offset.set(Duration.between(Instant.now(), target));
	}

	public void reset() {
		offset.set(Duration.ZERO);
	}

	@Override
	public Instant instant() {
		return Instant.now().plus(offset.get());
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException("The app always works in UTC");
	}

}
