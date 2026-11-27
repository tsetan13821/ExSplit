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
@Table(name = "group_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false, updatable = false)
	private Long groupId;

	@Column(name = "token_hash", nullable = false, unique = true, updatable = false)
	private String tokenHash;

	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "used_by")
	private Long usedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public GroupInvite(Long groupId, String tokenHash, Long createdBy, Instant expiresAt, Instant createdAt) {
		this.groupId = groupId;
		this.tokenHash = tokenHash;
		this.createdBy = createdBy;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

}
