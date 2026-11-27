package com.tenzin.splitter.settlement;

import java.util.List;

import com.tenzin.splitter.security.AuthUser;
import com.tenzin.splitter.settlement.SettlementDtos.MemberBalance;
import com.tenzin.splitter.settlement.SettlementDtos.RecordSettlementRequest;
import com.tenzin.splitter.settlement.SettlementDtos.SettlementResponse;
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
@RequestMapping("/api/groups/{groupId}")
public class SettlementController {

	private final BalanceService balanceService;

	private final SettlementService settlementService;

	public SettlementController(BalanceService balanceService, SettlementService settlementService) {
		this.balanceService = balanceService;
		this.settlementService = settlementService;
	}

	@GetMapping("/balances")
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public List<MemberBalance> balances(@PathVariable Long groupId) {
		return balanceService.balances(groupId);
	}

	@PostMapping("/settlements")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@groupSecurity.isMember(#groupId)")
	public SettlementResponse record(@PathVariable Long groupId, @Valid @RequestBody RecordSettlementRequest request,
			@AuthenticationPrincipal AuthUser user) {
		return settlementService.record(groupId, user.id(), request);
	}

}
