package com.tenzin.splitter.group;

import java.time.Clock;
import java.time.Instant;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.common.TokenGenerator;
import com.tenzin.splitter.group.GroupDtos.GroupDetails;
import com.tenzin.splitter.group.GroupDtos.InviteResponse;
import com.tenzin.splitter.user.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-use invite links. Same recipe as refresh tokens: random token in the link,
 * SHA-256 hash in the table, an expiry, and an atomic claim.
 */
@Service
public class InviteService {

	static final String INVITE_NOT_FOUND = "Invite not found";

	static final String INVITE_GONE = "Invite has expired or was already used";

	private final GroupInviteRepository invites;

	private final GroupMemberRepository members;

	private final GroupService groupService;

	private final UserRepository users;

	private final TokenGenerator tokenGenerator;

	private final GroupProperties properties;

	private final Clock clock;

	public InviteService(GroupInviteRepository invites, GroupMemberRepository members, GroupService groupService,
			UserRepository users, TokenGenerator tokenGenerator, GroupProperties properties, Clock clock) {
		this.invites = invites;
		this.members = members;
		this.groupService = groupService;
		this.users = users;
		this.tokenGenerator = tokenGenerator;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public InviteResponse create(long groupId, long ownerId) {
		String token = tokenGenerator.newToken();
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.inviteTtl());
		invites.save(new GroupInvite(groupId, TokenGenerator.sha256(token), ownerId, expiresAt, now));
		return new InviteResponse(token, "/api/invites/" + token + "/accept", expiresAt);
	}

	/**
	 * Joins the invite's group. Someone who is already a member is turned away before the
	 * invite is claimed, so their click does not burn a link meant for someone else.
	 */
	@Transactional
	public GroupDetails accept(String token, long userId) {
		GroupInvite invite = invites.findByTokenHash(TokenGenerator.sha256(token))
			.orElseThrow(() -> ApiException.notFound(INVITE_NOT_FOUND));
		long groupId = invite.getGroupId();

		if (members.isMember(groupId, userId)) {
			throw ApiException.conflict("You are already a member of this group");
		}
		Instant now = clock.instant();
		if (invites.claim(invite.getId(), userId, now) == 0) {
			throw new ApiException(HttpStatus.GONE, INVITE_GONE);
		}
		try {
			members.saveAndFlush(new GroupMember(groupId, users.getReferenceById(userId), GroupRole.MEMBER, now));
		}
		catch (DataIntegrityViolationException ex) {
			// Joined through another invite at the same moment. Rolling back un-claims this one.
			throw ApiException.conflict("You are already a member of this group");
		}
		return groupService.details(groupId);
	}

}
