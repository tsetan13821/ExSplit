package com.tenzin.splitter.settlement;

import java.time.Clock;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.group.GroupMemberRepository;
import com.tenzin.splitter.settlement.SettlementDtos.RecordSettlementRequest;
import com.tenzin.splitter.settlement.SettlementDtos.SettlementResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

	private final SettlementRepository settlements;

	private final GroupMemberRepository members;

	private final Clock clock;

	public SettlementService(SettlementRepository settlements, GroupMemberRepository members, Clock clock) {
		this.settlements = settlements;
		this.members = members;
		this.clock = clock;
	}

	/** Records that the caller paid {@code toUserId}. Both must be members of the group. */
	@Transactional
	public SettlementResponse record(long groupId, long fromUserId, RecordSettlementRequest request) {
		long toUserId = request.toUserId();
		if (toUserId == fromUserId) {
			throw ApiException.badRequest("You cannot settle up with yourself");
		}
		if (!members.isMember(groupId, toUserId)) {
			throw ApiException.badRequest("The person you paid must be a member of this group");
		}
		Settlement saved = settlements
			.save(new Settlement(groupId, fromUserId, toUserId, request.amountPaise(), clock.instant()));
		return SettlementResponse.from(saved);
	}

}
