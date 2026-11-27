package com.tenzin.splitter.expense;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false, updatable = false)
	private Long groupId;

	@Column(name = "paid_by", nullable = false)
	private Long paidBy;

	@Column(nullable = false)
	private String description;

	@Column(name = "amount_paise", nullable = false)
	private long amountPaise;

	@Enumerated(EnumType.STRING)
	@Column(name = "split_type", nullable = false, length = 10)
	private SplitType splitType;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Shares live and die with the expense, so they are values rather than entities. */
	@ElementCollection
	@CollectionTable(name = "expense_shares", joinColumns = @JoinColumn(name = "expense_id"))
	private List<ExpenseShare> shares = new ArrayList<>();

	public Expense(Long groupId, Long paidBy, String description, long amountPaise, SplitType splitType,
			LocalDate expenseDate, Long createdBy, Instant createdAt) {
		this.groupId = groupId;
		this.paidBy = paidBy;
		this.description = description;
		this.amountPaise = amountPaise;
		this.splitType = splitType;
		this.expenseDate = expenseDate;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
	}

	public void addShare(Long userId, long sharePaise) {
		shares.add(new ExpenseShare(userId, sharePaise));
	}

	public List<ExpenseShare> getShares() {
		return Collections.unmodifiableList(shares);
	}

}
