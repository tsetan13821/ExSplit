package com.tenzin.splitter.expense;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ExpenseDtos {

	private ExpenseDtos() {
	}

	/**
	 * For EQUAL send {@code participantIds}; for EXACT send {@code shares}. Never both.
	 * {@code expenseDate} defaults to today.
	 */
	public record CreateExpenseRequest(
			@NotBlank @Size(max = 200) String description,
			@NotNull @Positive Long amountPaise,
			@NotNull Long paidBy,
			@NotNull SplitType splitType,
			LocalDate expenseDate,
			@Size(max = 100) List<Long> participantIds,
			@Size(max = 100) List<@Valid @NotNull ShareRequest> shares) {
	}

	public record ShareRequest(@NotNull Long userId, @NotNull @PositiveOrZero Long sharePaise) {
	}

	public record ShareResponse(Long userId, Long sharePaise) {
	}

	public record ExpenseResponse(Long id, Long groupId, String description, Long amountPaise, Long paidBy,
			SplitType splitType, LocalDate expenseDate, Long createdBy, Instant createdAt,
			List<ShareResponse> shares) {

		static ExpenseResponse from(Expense expense) {
			List<ShareResponse> shares = expense.getShares()
				.stream()
				.map(s -> new ShareResponse(s.userId(), s.sharePaise()))
				.toList();
			return new ExpenseResponse(expense.getId(), expense.getGroupId(), expense.getDescription(),
					expense.getAmountPaise(), expense.getPaidBy(), expense.getSplitType(), expense.getExpenseDate(),
					expense.getCreatedBy(), expense.getCreatedAt(), shares);
		}

	}

}
