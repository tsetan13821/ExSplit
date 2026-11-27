package com.tenzin.splitter.recurring;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RecurringDtos {

	private RecurringDtos() {
	}

	/** Posted monthly on {@code dayOfMonth}, split equally among the group's members at that time. */
	public record CreateRecurringExpenseRequest(
			@NotBlank @Size(max = 200) String description,
			@NotNull @Positive Long amountPaise,
			@NotNull Long paidBy,
			@NotNull @Min(1) @Max(28) Integer dayOfMonth) {
	}

	public record RecurringExpenseResponse(Long id, Long groupId, String description, long amountPaise, Long paidBy,
			int dayOfMonth, LocalDate nextRunDate, boolean active, Long createdBy, Instant createdAt) {

		static RecurringExpenseResponse from(RecurringExpense r) {
			return new RecurringExpenseResponse(r.getId(), r.getGroupId(), r.getDescription(), r.getAmountPaise(),
					r.getPaidBy(), r.getDayOfMonth(), r.getNextRunDate(), r.isActive(), r.getCreatedBy(),
					r.getCreatedAt());
		}

	}

}
