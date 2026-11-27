package com.tenzin.splitter.settlement;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class SettlementDtos {

	private SettlementDtos() {
	}

	/**
	 * @param netPaise positive: the group owes this member; negative: this member owes the group
	 */
	public record MemberBalance(Long userId, String name, long netPaise) {
	}

	/** The payer is always the caller, taken from the token, never from the body. */
	public record RecordSettlementRequest(@NotNull Long toUserId, @NotNull @Positive Long amountPaise) {
	}

	public record SettlementResponse(Long id, Long groupId, Long fromUserId, Long toUserId, long amountPaise,
			Instant createdAt) {

		static SettlementResponse from(Settlement s) {
			return new SettlementResponse(s.getId(), s.getGroupId(), s.getFromUser(), s.getToUser(),
					s.getAmountPaise(), s.getCreatedAt());
		}

	}

}
