package com.tenzin.splitter.expense;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.common.PageResponse;
import com.tenzin.splitter.expense.ExpenseDtos.CreateExpenseRequest;
import com.tenzin.splitter.expense.ExpenseDtos.ExpenseResponse;
import com.tenzin.splitter.expense.ExpenseDtos.ShareRequest;
import com.tenzin.splitter.group.GroupMemberRepository;
import com.tenzin.splitter.group.GroupRole;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces the rules the database cannot: shares add up to the amount (SplitCalculator),
 * and the payer and every share holder are members of the expense's group.
 */
@Service
public class ExpenseService {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"));

	private final ExpenseRepository expenses;

	private final GroupMemberRepository members;

	private final Clock clock;

	public ExpenseService(ExpenseRepository expenses, GroupMemberRepository members, Clock clock) {
		this.expenses = expenses;
		this.members = members;
		this.clock = clock;
	}

	@Transactional
	public ExpenseResponse create(long groupId, long actorId, CreateExpenseRequest request) {
		Map<Long, Long> shares = computeShares(request);

		Set<Long> involved = new HashSet<>(shares.keySet());
		involved.add(request.paidBy());
		if (members.countMembersAmong(groupId, involved) != involved.size()) {
			throw ApiException.badRequest("The payer and every participant must be members of this group");
		}

		LocalDate date = request.expenseDate() != null ? request.expenseDate() : LocalDate.now(clock);
		Expense expense = new Expense(groupId, request.paidBy(), request.description().trim(), request.amountPaise(),
				request.splitType(), date, actorId, clock.instant());
		shares.forEach(expense::addShare);
		return ExpenseResponse.from(expenses.save(expense));
	}

	@Transactional(readOnly = true)
	public PageResponse<ExpenseResponse> list(long groupId, int page, int size) {
		return PageResponse.from(expenses.findByGroupId(groupId, PageRequest.of(page, size, NEWEST_FIRST)),
				ExpenseResponse::from);
	}

	/** Allowed for whoever created the expense, or the group owner. */
	@Transactional
	public void delete(long groupId, long expenseId, long actorId) {
		Expense expense = expenses.findByIdAndGroupId(expenseId, groupId)
			.orElseThrow(() -> ApiException.notFound("Expense not found"));
		if (expense.getCreatedBy() != actorId && !members.hasRole(groupId, actorId, GroupRole.OWNER)) {
			throw ApiException.forbidden("Only the person who added this expense or the group owner can delete it");
		}
		expenses.delete(expense);
	}

	private static Map<Long, Long> computeShares(CreateExpenseRequest request) {
		boolean hasParticipants = request.participantIds() != null && !request.participantIds().isEmpty();
		boolean hasShares = request.shares() != null && !request.shares().isEmpty();
		try {
			return switch (request.splitType()) {
				case EQUAL -> {
					if (hasShares) {
						throw new IllegalArgumentException("An equal split takes participantIds, not shares");
					}
					yield SplitCalculator.equal(request.amountPaise(), request.participantIds());
				}
				case EXACT -> {
					if (hasParticipants) {
						throw new IllegalArgumentException("An exact split takes shares, not participantIds");
					}
					yield SplitCalculator.exact(request.amountPaise(), toShareMap(request.shares()));
				}
			};
		}
		catch (IllegalArgumentException ex) {
			throw ApiException.badRequest(ex.getMessage());
		}
	}

	private static Map<Long, Long> toShareMap(List<ShareRequest> shares) {
		if (shares == null) {
			return Map.of();
		}
		Map<Long, Long> map = new LinkedHashMap<>();
		for (ShareRequest share : shares) {
			if (map.put(share.userId(), share.sharePaise()) != null) {
				throw new IllegalArgumentException("User " + share.userId() + " has more than one share");
			}
		}
		return map;
	}

}
