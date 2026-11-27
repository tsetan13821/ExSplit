package com.tenzin.splitter.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tenzin.splitter.settlement.DebtSimplifier.Transfer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

/** Phase 5 "done when": all of these pass. */
class DebtSimplifierTest {

	private static final long YOU = 1;

	private static final long A = 2;

	private static final long B = 3;

	@Test
	void goaTripNeedsTwoPayments() {
		List<Transfer> transfers = DebtSimplifier.simplify(Map.of(YOU, 150_000L, A, -60_000L, B, -90_000L));

		assertThat(transfers).containsExactly(new Transfer(B, YOU, 90_000), new Transfer(A, YOU, 60_000));
	}

	@Test
	void everyoneAtZeroNeedsNoPayments() {
		assertThat(DebtSimplifier.simplify(Map.of(YOU, 0L, A, 0L, B, 0L))).isEmpty();
	}

	@Test
	void emptyGroupNeedsNoPayments() {
		assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
	}

	@Test
	void oneDebtorPaysEveryCreditorExactlyWhatTheyAreOwed() {
		Map<Long, Long> net = Map.of(10L, -600L, 11L, 100L, 12L, 200L, 13L, 300L);

		List<Transfer> transfers = DebtSimplifier.simplify(net);

		assertThat(transfers).containsExactly(new Transfer(10, 13, 300), new Transfer(10, 12, 200),
				new Transfer(10, 11, 100));
	}

	@Test
	void manyDebtorsPayOneCreditor() {
		List<Transfer> transfers = DebtSimplifier.simplify(Map.of(1L, 600L, 2L, -100L, 3L, -200L, 4L, -300L));

		assertThat(transfers).hasSize(3).allSatisfy(t -> assertThat(t.toUserId()).isEqualTo(1));
		assertSettlesEveryone(Map.of(1L, 600L, 2L, -100L, 3L, -200L, 4L, -300L), transfers);
	}

	@Test
	void chainCollapsesIntoOnePayment() {
		// A owes B 100 and B owes C 100: on balance A owes C directly.
		assertThat(DebtSimplifier.simplify(Map.of(A, -100L, B, 0L, YOU, 100L)))
			.containsExactly(new Transfer(A, YOU, 100));
	}

	@Test
	void tiesAreBrokenByUserIdSoOutputIsStable() {
		Map<Long, Long> net = new LinkedHashMap<>();
		net.put(9L, -50L);
		net.put(3L, -50L);
		net.put(7L, 50L);
		net.put(5L, 50L);

		assertThat(DebtSimplifier.simplify(net)).containsExactly(new Transfer(3, 5, 50), new Transfer(9, 7, 50));
	}

	@Test
	void rejectsBalancesThatDoNotSumToZero() {
		assertThatThrownBy(() -> DebtSimplifier.simplify(Map.of(A, 100L, B, -90L)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("sum to 10");
	}

	/**
	 * Random groups: the payments always clear every balance, in at most n - 1 steps, and
	 * money only ever flows from people who owe to people who are owed.
	 */
	@RepeatedTest(500)
	void randomGroupsAreAlwaysFullySettledInAtMostNMinusOnePayments(RepetitionInfo repetition) {
		Random random = new Random(repetition.getCurrentRepetition());
		int people = 2 + random.nextInt(40);
		Map<Long, Long> net = new HashMap<>();
		long sum = 0;
		for (long user = 1; user < people; user++) {
			long amount = random.nextInt(5) == 0 ? 0 : random.nextLong(-1_000_000, 1_000_000);
			net.put(user, amount);
			sum += amount;
		}
		net.put((long) people, -sum); // last person balances the books

		List<Transfer> transfers = DebtSimplifier.simplify(net);

		long nonZero = net.values().stream().filter(v -> v != 0).count();
		assertThat(transfers).hasSizeLessThanOrEqualTo((int) Math.max(0, nonZero - 1));
		for (Transfer t : transfers) {
			assertThat(t.amountPaise()).isPositive();
			assertThat(net.get(t.fromUserId())).isNegative();
			assertThat(net.get(t.toUserId())).isPositive();
		}
		assertSettlesEveryone(net, transfers);
	}

	private static void assertSettlesEveryone(Map<Long, Long> net, List<Transfer> transfers) {
		Map<Long, Long> after = new HashMap<>(net);
		for (Transfer t : transfers) {
			after.merge(t.fromUserId(), t.amountPaise(), Long::sum); // paying reduces what you owe
			after.merge(t.toUserId(), -t.amountPaise(), Long::sum); // receiving reduces what you are owed
		}
		assertThat(after.values()).allMatch(v -> v == 0);
	}

}
