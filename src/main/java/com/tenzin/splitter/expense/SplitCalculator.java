package com.tenzin.splitter.expense;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Works out each person's share of an expense, in paise. Plain Java with no Spring
 * dependencies, so it is unit-tested directly. Invalid input throws
 * {@link IllegalArgumentException}; the service turns that into a 400.
 *
 * Both methods return shares ordered by user id and always summing exactly to the amount.
 */
public final class SplitCalculator {

	private SplitCalculator() {
	}

	/**
	 * Splits evenly. The remainder that cannot be divided is handed out one paisa at a
	 * time to the lowest user ids: ₹100 three ways is 3334, 3333, 3333.
	 */
	public static Map<Long, Long> equal(long amountPaise, Collection<Long> participantIds) {
		requirePositive(amountPaise);
		if (participantIds == null || participantIds.isEmpty()) {
			throw new IllegalArgumentException("An equal split needs at least one participant");
		}
		TreeSet<Long> sorted = new TreeSet<>();
		for (Long id : participantIds) {
			if (id == null) {
				throw new IllegalArgumentException("Participant ids must not be null");
			}
			if (!sorted.add(id)) {
				throw new IllegalArgumentException("Participant " + id + " is listed more than once");
			}
		}

		long n = sorted.size();
		long base = amountPaise / n;
		long remainder = amountPaise % n;

		Map<Long, Long> shares = new LinkedHashMap<>();
		long index = 0;
		for (Long id : sorted) {
			shares.put(id, base + (index < remainder ? 1 : 0));
			index++;
		}
		return shares;
	}

	/** Uses the given shares as-is, after checking they add up exactly to the amount. */
	public static Map<Long, Long> exact(long amountPaise, Map<Long, Long> requestedShares) {
		requirePositive(amountPaise);
		if (requestedShares == null || requestedShares.isEmpty()) {
			throw new IllegalArgumentException("An exact split needs at least one share");
		}
		Map<Long, Long> shares = new TreeMap<>();
		long total = 0;
		for (Map.Entry<Long, Long> entry : requestedShares.entrySet()) {
			Long userId = entry.getKey();
			Long share = entry.getValue();
			if (userId == null || share == null) {
				throw new IllegalArgumentException("Every share needs a user id and an amount");
			}
			if (share < 0) {
				throw new IllegalArgumentException("Shares cannot be negative");
			}
			try {
				total = Math.addExact(total, share);
			}
			catch (ArithmeticException ex) {
				throw new IllegalArgumentException("Shares are too large");
			}
			shares.put(userId, share);
		}
		if (total != amountPaise) {
			throw new IllegalArgumentException(
					"Shares add up to " + total + " paise but the expense is " + amountPaise + " paise");
		}
		return new LinkedHashMap<>(shares);
	}

	private static void requirePositive(long amountPaise) {
		if (amountPaise <= 0) {
			throw new IllegalArgumentException("Amount must be positive");
		}
	}

}
