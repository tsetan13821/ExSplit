package com.tenzin.splitter.group;

import com.tenzin.splitter.group.GroupDtos.GroupDetails;
import com.tenzin.splitter.security.AuthUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

	private final InviteService inviteService;

	public InviteController(InviteService inviteService) {
		this.inviteService = inviteService;
	}

	/** Any logged-in user holding the link can join; the token itself is the permission. */
	@PostMapping("/{token}/accept")
	public GroupDetails accept(@PathVariable String token, @AuthenticationPrincipal AuthUser user) {
		return inviteService.accept(token, user.id());
	}

}
