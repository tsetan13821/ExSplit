package com.tenzin.splitter.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SplitCalculatorTest {

	@Test
	void hundredRupeesThreeWaysIs3334_3333_3333() {
		Map<Long, Long> shares = SplitCalculator.equal(10_000, List.of(30L, 10L, 20L));

		assertThat(shares).containsExactly(Map.entry(10L, 3334L), Map.entry(20L, 3333L), Map.entry(30L, 3333L));
	}

	@Test
	void remainderGoesToLowestIdsRegardlessOfInputOrder() {
		assertThat(SplitCalculator.equal(10, List.of(4L, 3L, 2L, 1L)))
			.containsExactly(Map.entry(1L, 3L), Map.entry(2L, 3L), Map.entry(3L, 2L), Map.entry(4L, 2L));
	}

	@Test
	void evenAmountSplitsEvenly() {
		assertThat(SplitCalculator.equal(900, List.of(1L, 2L, 3L))).containsOnly(Map.entry(1L, 300L),
				Map.entry(2L, 300L), Map.entry(3L, 300L));
	}

	@Test
	void amountSmallerThanParticipantsGivesSomeZeroShares() {
		assertThat(SplitCalculator.equal(1, List.of(1L, 2L, 3L))).containsExactly(Map.entry(1L, 1L),
				Map.entry(2L, 0L), Map.entry(3L, 0L));
	}

	@Test
	void equalSharesAlwaysSumToTheAmount() {
		Random random = new Random(42);
		for (int i = 0; i < 1_000; i++) {
			long amount = 1 + random.nextInt(10_000_000);
			List<Long> people = random.longs(1 + random.nextInt(20), 1, 1_000_000).distinct().boxed().toList();
			Map<Long, Long> shares = SplitCalculator.equal(amount, people);

			assertThat(shares.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(amount);
			long max = shares.values().stream().mapToLong(Long::longValue).max().orElseThrow();
			long min = shares.values().stream().mapToLong(Long::longValue).min().orElseThrow();
			assertThat(max - min).isLessThanOrEqualTo(1);
		}
	}

	@Test
	void equalRejectsDuplicatesEmptyListAndNonPositiveAmounts() {
		assertThatThrownBy(() -> SplitCalculator.equal(100, List.of(1L, 1L))).hasMessageContaining("more than once");
		assertThatThrownBy(() -> SplitCalculator.equal(100, List.of())).hasMessageContaining("at least one");
		assertThatThrownBy(() -> SplitCalculator.equal(0, List.of(1L))).hasMessageContaining("positive");
		assertThatThrownBy(() -> SplitCalculator.equal(-5, List.of(1L))).hasMessageContaining("positive");
	}

	@Test
	void exactAcceptsSharesThatAddUp() {
		Map<Long, Long> requested = new LinkedHashMap<>();
		requested.put(44L, 20_000L);
		requested.put(42L, 30_000L);
		requested.put(43L, 40_000L);

		assertThat(SplitCalculator.exact(90_000, requested)).containsExactly(Map.entry(42L, 30_000L),
				Map.entry(43L, 40_000L), Map.entry(44L, 20_000L));
	}

	@Test
	void exactRejectsSharesThatDoNotAddUp() {
		assertThatThrownBy(() -> SplitCalculator.exact(90_000, Map.of(1L, 30_000L, 2L, 40_000L)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("add up to 70000");
	}

	@Test
	void exactRejectsNegativeSharesEvenIfTheTotalMatches() {
		assertThatThrownBy(() -> SplitCalculator.exact(100, Map.of(1L, 150L, 2L, -50L)))
			.hasMessageContaining("negative");
	}

	@Test
	void exactRejectsOverflowingShares() {
		assertThatThrownBy(() -> SplitCalculator.exact(100, Map.of(1L, Long.MAX_VALUE, 2L, Long.MAX_VALUE)))
			.hasMessageContaining("too large");
	}

}
