package com.tenzin.splitter.recurring;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recurring_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringExpense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false, updatable = false)
	private Long groupId;

	@Column(name = "paid_by", nullable = false, updatable = false)
	private Long paidBy;

	@Column(nullable = false, updatable = false)
	private String description;

	@Column(name = "amount_paise", nullable = false, updatable = false)
	private long amountPaise;

	@Column(name = "day_of_month", nullable = false, updatable = false)
	private int dayOfMonth;

	@Column(name = "next_run_date", nullable = false)
	private LocalDate nextRunDate;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public RecurringExpense(Long groupId, Long paidBy, String description, long amountPaise, int dayOfMonth,
			LocalDate today, Long createdBy, Instant createdAt) {
		this.groupId = groupId;
		this.paidBy = paidBy;
		this.description = description;
		this.amountPaise = amountPaise;
		this.dayOfMonth = dayOfMonth;
		this.nextRunDate = firstRunDate(today, dayOfMonth);
		this.createdBy = createdBy;
		this.createdAt = createdAt;
	}

	/** This month's day if it has not passed yet (today counts), otherwise next month's. */
	static LocalDate firstRunDate(LocalDate today, int dayOfMonth) {
		LocalDate thisMonth = today.withDayOfMonth(dayOfMonth);
		return thisMonth.isBefore(today) ? thisMonth.plusMonths(1) : thisMonth;
	}

	/** Safe for every month because day_of_month is at most 28. */
	void advanceOneMonth() {
		nextRunDate = nextRunDate.plusMonths(1);
	}

	void stop() {
		active = false;
	}

}
