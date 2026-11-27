package com.tenzin.splitter.group;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.group.GroupDtos.GroupDetails;
import com.tenzin.splitter.group.GroupDtos.GroupSummary;
import com.tenzin.splitter.group.GroupDtos.MemberResponse;
import com.tenzin.splitter.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Group business logic. Callers are expected to have passed the membership check in
 * {@code GroupSecurity} already; this class does not repeat it.
 */
@Service
public class GroupService {

	private final ExpenseGroupRepository groups;

	private final GroupMemberRepository members;

	private final UserRepository users;

	private final Clock clock;

	public GroupService(ExpenseGroupRepository groups, GroupMemberRepository members, UserRepository users,
			Clock clock) {
		this.groups = groups;
		this.members = members;
		this.users = users;
		this.clock = clock;
	}

	/** The creator becomes the group's OWNER in the same transaction. */
	@Transactional
	public GroupDetails create(String name, long creatorId) {
		Instant now = clock.instant();
		ExpenseGroup group = groups.save(new ExpenseGroup(name.trim(), creatorId, now));
		members.saveAndFlush(new GroupMember(group.getId(), users.getReferenceById(creatorId), GroupRole.OWNER, now));
		return details(group.getId());
	}

	@Transactional(readOnly = true)
	public List<GroupSummary> listForUser(long userId) {
		return groups.findSummariesForUser(userId);
	}

	@Transactional(readOnly = true)
	public GroupDetails details(long groupId) {
		ExpenseGroup group = groups.findById(groupId).orElseThrow(() -> ApiException.notFound("Group not found"));
		List<MemberResponse> memberList = members.findMembersWithUsers(groupId)
			.stream()
			.map(MemberResponse::from)
			.toList();
		return new GroupDetails(group.getId(), group.getName(), group.getCreatedBy(), group.getCreatedAt(), memberList);
	}

}
