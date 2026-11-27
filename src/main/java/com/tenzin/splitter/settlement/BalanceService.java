package com.tenzin.splitter.settlement;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tenzin.splitter.common.UserAmount;
import com.tenzin.splitter.expense.ExpenseRepository;
import com.tenzin.splitter.group.GroupMember;
import com.tenzin.splitter.group.GroupMemberRepository;
import com.tenzin.splitter.settlement.SettlementDtos.MemberBalance;
import com.tenzin.splitter.settlement.SettlementDtos.SuggestedTransfer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * For each member:
 * <pre>net = paid for expenses - own shares + settlements sent - settlements received</pre>
 * Each term is one {@code GROUP BY user} query, so the cost does not grow with the
 * number of expenses held in memory. Nets of a group always sum to zero.
 */
@Service
public class BalanceService {

	private final ExpenseRepository expenses;

	private final SettlementRepository settlements;

	private final GroupMemberRepository members;

	public BalanceService(ExpenseRepository expenses, SettlementRepository settlements,
			GroupMemberRepository members) {
		this.expenses = expenses;
		this.settlements = settlements;
		this.members = members;
	}

	/** Net per user id, every member included (zero if they have no activity). */
	@Transactional(readOnly = true)
	public Map<Long, Long> netByUser(long groupId) {
		Map<Long, Long> net = new LinkedHashMap<>();
		for (GroupMember member : members.findMembersWithUsers(groupId)) {
			net.put(member.getId().userId(), 0L);
		}
		add(net, expenses.sumPaidByUser(groupId), 1);
		add(net, expenses.sumSharesByUser(groupId), -1);
		add(net, settlements.sumSentByUser(groupId), 1);
		add(net, settlements.sumReceivedByUser(groupId), -1);

		long total = net.values().stream().mapToLong(Long::longValue).sum();
		if (total != 0) {
			// Every paisa paid is someone's share and every settlement has two sides, so this
			// can only mean corrupted data. Fail loudly rather than show wrong balances.
			throw new IllegalStateException("Balances of group " + groupId + " sum to " + total + ", not 0");
		}
		return net;
	}

	@Transactional(readOnly = true)
	public List<MemberBalance> balances(long groupId) {
		Map<Long, String> names = memberNames(groupId);
		return netByUser(groupId).entrySet()
			.stream()
			.map(e -> new MemberBalance(e.getKey(), names.get(e.getKey()), e.getValue()))
			.toList();
	}

	/** The fewest payments (greedy, at most n - 1) that bring every balance to zero. */
	@Transactional(readOnly = true)
	public List<SuggestedTransfer> suggestedSettlements(long groupId) {
		Map<Long, String> names = memberNames(groupId);
		return DebtSimplifier.simplify(netByUser(groupId))
			.stream()
			.map(t -> new SuggestedTransfer(t.fromUserId(), names.get(t.fromUserId()), t.toUserId(),
					names.get(t.toUserId()), t.amountPaise()))
			.toList();
	}

	private Map<Long, String> memberNames(long groupId) {
		Map<Long, String> names = new HashMap<>();
		for (GroupMember member : members.findMembersWithUsers(groupId)) {
			names.put(member.getId().userId(), member.getUser().getName());
		}
		return names;
	}

	private static void add(Map<Long, Long> net, List<UserAmount> amounts, int sign) {
		for (UserAmount row : amounts) {
			net.merge(row.userId(), sign * row.amountPaise(), Math::addExact);
		}
	}

}
