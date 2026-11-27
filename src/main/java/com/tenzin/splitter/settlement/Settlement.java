package com.tenzin.splitter.settlement;

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

/** A real payment made outside the app: {@code fromUser} paid {@code toUser}. */
@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false, updatable = false)
	private Long groupId;

	@Column(name = "from_user", nullable = false, updatable = false)
	private Long fromUser;

	@Column(name = "to_user", nullable = false, updatable = false)
	private Long toUser;

	@Column(name = "amount_paise", nullable = false, updatable = false)
	private long amountPaise;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Settlement(Long groupId, Long fromUser, Long toUser, long amountPaise, Instant createdAt) {
		this.groupId = groupId;
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.amountPaise = amountPaise;
		this.createdAt = createdAt;
	}

}
