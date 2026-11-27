package com.tenzin.splitter.group;

import java.util.List;

import com.tenzin.splitter.group.GroupDtos.CreateGroupRequest;
import com.tenzin.splitter.group.GroupDtos.GroupDetails;
import com.tenzin.splitter.group.GroupDtos.GroupSummary;
import com.tenzin.splitter.group.GroupDtos.InviteResponse;
import com.tenzin.splitter.security.AuthUser;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

	private final GroupService groupService;

	private final InviteService inviteService;

	public GroupController(GroupService groupService, InviteService inviteService) {
		this.groupService = groupService;
		this.inviteService = inviteService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GroupDetails create(@Valid @RequestBody CreateGroupRequest request,
			@AuthenticationPrincipal AuthUser user) {
		return groupService.create(request.name(), user.id());
	}

	/** Only the caller's own groups: there is no endpoint that lists every group. */
	@GetMapping
	public List<GroupSummary> myGroups(@AuthenticationPrincipal AuthUser user) {
		return groupService.listForUser(user.id());
	}

	@GetMapping("/{groupId}")
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public GroupDetails get(@PathVariable Long groupId) {
		return groupService.details(groupId);
	}

	@PostMapping("/{groupId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@groupSecurity.isOwner(#groupId)")
	public InviteResponse createInvite(@PathVariable Long groupId, @AuthenticationPrincipal AuthUser user) {
		return inviteService.create(groupId, user.id());
	}

}
