package com.tenzin.splitter.settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Turns net balances into a short list of payments that settles everyone.
 *
 * <p>Greedy with two max-heaps: repeatedly take the person owed the most and the person
 * owing the most, and have the debtor pay the smaller of the two amounts. That payment
 * settles at least one of them fully, so with n people holding a non-zero balance there
 * are at most n - 1 payments, in O(n log n) time.
 *
 * <p>Not always optimal: finding the true minimum number of payments is NP-hard (it
 * contains subset-sum: any group of people whose balances cancel out can settle among
 * themselves). The greedy result is a deliberate trade-off: fast, simple and never worse
 * than n - 1.
 *
 * <p>Plain Java with no Spring dependencies, so it is unit-tested directly.
 */
public final class DebtSimplifier {

	/** {@code fromUserId} pays {@code toUserId}. */
	public record Transfer(long fromUserId, long toUserId, long amountPaise) {
	}

	private record Party(long userId, long amountPaise) {
	}

	/** Largest amount first; ties go to the lower user id so the output is deterministic. */
	private static final Comparator<Party> LARGEST_FIRST = Comparator.comparingLong(Party::amountPaise)
		.reversed()
		.thenComparingLong(Party::userId);

	private DebtSimplifier() {
	}

	/**
	 * @param netByUser positive: owed money; negative: owes money. Must sum to zero.
	 * @throws IllegalArgumentException if the balances do not sum to zero
	 */
	public static List<Transfer> simplify(Map<Long, Long> netByUser) {
		PriorityQueue<Party> creditors = new PriorityQueue<>(LARGEST_FIRST);
		PriorityQueue<Party> debtors = new PriorityQueue<>(LARGEST_FIRST);
		long total = 0;
		for (Map.Entry<Long, Long> entry : netByUser.entrySet()) {
			long net = entry.getValue();
			total = Math.addExact(total, net);
			if (net > 0) {
				creditors.add(new Party(entry.getKey(), net));
			}
			else if (net < 0) {
				debtors.add(new Party(entry.getKey(), Math.negateExact(net)));
			}
		}
		if (total != 0) {
			throw new IllegalArgumentException("Balances must sum to zero, but sum to " + total);
		}

		List<Transfer> transfers = new ArrayList<>();
		while (!creditors.isEmpty() && !debtors.isEmpty()) {
			Party creditor = creditors.poll();
			Party debtor = debtors.poll();
			long pay = Math.min(creditor.amountPaise(), debtor.amountPaise());
			transfers.add(new Transfer(debtor.userId(), creditor.userId(), pay));

			if (creditor.amountPaise() > pay) {
				creditors.add(new Party(creditor.userId(), creditor.amountPaise() - pay));
			}
			if (debtor.amountPaise() > pay) {
				debtors.add(new Party(debtor.userId(), debtor.amountPaise() - pay));
			}
		}
		return transfers;
	}

}
