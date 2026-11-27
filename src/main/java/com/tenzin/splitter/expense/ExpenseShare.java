package com.tenzin.splitter.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One person's part of an expense. Stored in expense_shares, owned by {@link Expense}. */
@Embeddable
public record ExpenseShare(
		@Column(name = "user_id", nullable = false) Long userId,
		@Column(name = "share_paise", nullable = false) Long sharePaise) {
}
