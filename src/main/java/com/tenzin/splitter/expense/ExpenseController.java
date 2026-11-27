package com.tenzin.splitter.expense;

import com.tenzin.splitter.common.PageResponse;
import com.tenzin.splitter.expense.ExpenseDtos.CreateExpenseRequest;
import com.tenzin.splitter.expense.ExpenseDtos.ExpenseResponse;
import com.tenzin.splitter.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public ExpenseResponse create(@PathVariable Long groupId, @Valid @RequestBody CreateExpenseRequest request,
			@AuthenticationPrincipal AuthUser user) {
		return expenseService.create(groupId, user.id(), request);
	}

	/** Newest first. */
	@GetMapping
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public PageResponse<ExpenseResponse> list(@PathVariable Long groupId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return expenseService.list(groupId, page, size);
	}

	/** Members pass the filter here; the service then allows only the creator or the owner. */
	@DeleteMapping("/{expenseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public void delete(@PathVariable Long groupId, @PathVariable Long expenseId,
			@AuthenticationPrincipal AuthUser user) {
		expenseService.delete(groupId, expenseId, user.id());
	}

}
