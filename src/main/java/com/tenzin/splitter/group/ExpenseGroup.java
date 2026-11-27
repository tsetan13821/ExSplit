package com.tenzin.splitter.group;

import java.time.Instant;

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
@Table(name = "expense_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public ExpenseGroup(String name, Long createdBy, Instant createdAt) {
		this.name = name;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
	}

}
