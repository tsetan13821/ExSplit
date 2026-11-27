package com.tenzin.splitter.group;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for the group and invite endpoints. */
public final class GroupDtos {

	private GroupDtos() {
	}

	public record CreateGroupRequest(@NotBlank @Size(max = 100) String name) {
	}

	/** One row of "my groups", with the caller's role in it. */
	public record GroupSummary(Long id, String name, GroupRole role, Instant createdAt) {
	}

	public record GroupDetails(Long id, String name, Long createdBy, Instant createdAt, List<MemberResponse> members) {
	}

	public record MemberResponse(Long userId, String name, String email, GroupRole role, Instant joinedAt) {

		static MemberResponse from(GroupMember member) {
			return new MemberResponse(member.getId().userId(), member.getUser().getName(),
					member.getUser().getEmail(), member.getRole(), member.getJoinedAt());
		}

	}

	/**
	 * The raw token is returned once, here, and never stored. Share {@code acceptPath}
	 * with the person being invited.
	 */
	public record InviteResponse(String token, String acceptPath, Instant expiresAt) {
	}

}
