package com.tenzin.splitter.group;

import java.time.Instant;

import com.tenzin.splitter.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Persistable;

/**
 * Implements {@link Persistable} because the id is assigned, not generated. Without it
 * Spring Data would treat a new member as existing and merge it, silently turning a
 * duplicate join into an update instead of failing on the primary key.
 */
@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember implements Persistable<GroupMemberId> {

	@EmbeddedId
	private GroupMemberId id;

	/**
	 * The key owns the user_id column; this mapping is read-only. It is still set in the
	 * constructor, because a member saved in this transaction is the same object later
	 * queries return, and Hibernate does not fill in a read-only association on save.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GroupRole role;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@Transient
	private boolean isNew = true;

	public GroupMember(Long groupId, User user, GroupRole role, Instant joinedAt) {
		this.id = new GroupMemberId(groupId, user.getId());
		this.user = user;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

}
