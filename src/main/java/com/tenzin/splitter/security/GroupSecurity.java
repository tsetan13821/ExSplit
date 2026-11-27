package com.tenzin.splitter.security;

import com.tenzin.splitter.group.GroupMemberRepository;
import com.tenzin.splitter.group.GroupRole;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Group-level authorization for {@code @PreAuthorize}, e.g.
 * {@code @PreAuthorize("@groupSecurity.isMember(#groupId)")}.
 *
 * Prevents IDOR: without it, any logged-in user could read another group by changing the
 * id in the URL. Membership is read from the database on every call rather than from the
 * token, so being added or removed takes effect immediately. A group that does not exist
 * answers the same as one you are not in (403), so ids cannot be probed.
 */
@Component("groupSecurity")
public class GroupSecurity {

	private final GroupMemberRepository members;

	public GroupSecurity(GroupMemberRepository members) {
		this.members = members;
	}

	public boolean isMember(Long groupId) {
		AuthUser user = currentUser();
		return user != null && groupId != null && members.isMember(groupId, user.id());
	}

	public boolean isOwner(Long groupId) {
		AuthUser user = currentUser();
		return user != null && groupId != null && members.hasRole(groupId, user.id(), GroupRole.OWNER);
	}

	private static AuthUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof AuthUser user ? user : null;
	}

}
