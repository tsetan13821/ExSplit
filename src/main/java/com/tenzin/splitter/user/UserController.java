package com.tenzin.splitter.user;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.security.AuthUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserRepository users;

	public UserController(UserRepository users) {
		this.users = users;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AuthUser principal) {
		return users.findById(principal.id())
			.map(UserResponse::from)
			.orElseThrow(() -> ApiException.notFound("User no longer exists"));
	}

}
