package com.tenzin.splitter.group;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GroupMemberId(
		@Column(name = "group_id") Long groupId,
		@Column(name = "user_id") Long userId) implements Serializable {
}
