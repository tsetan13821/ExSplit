package com.tenzin.splitter.recurring;

import java.util.List;

import com.tenzin.splitter.recurring.RecurringDtos.CreateRecurringExpenseRequest;
import com.tenzin.splitter.recurring.RecurringDtos.RecurringExpenseResponse;
import com.tenzin.splitter.security.AuthUser;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/recurring-expenses")
public class RecurringExpenseController {

	private final RecurringExpenseService service;

	public RecurringExpenseController(RecurringExpenseService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public RecurringExpenseResponse create(@PathVariable Long groupId,
			@Valid @RequestBody CreateRecurringExpenseRequest request, @AuthenticationPrincipal AuthUser user) {
		return service.create(groupId, user.id(), request);
	}

	@GetMapping
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public List<RecurringExpenseResponse> list(@PathVariable Long groupId) {
		return service.listActive(groupId);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@groupSecurity.isOwner(#groupId)")
	public void stop(@PathVariable Long groupId, @PathVariable Long id) {
		service.stop(groupId, id);
	}

}
