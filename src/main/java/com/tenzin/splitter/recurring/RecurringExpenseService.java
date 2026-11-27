package com.tenzin.splitter.recurring;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.expense.ExpenseDtos.CreateExpenseRequest;
import com.tenzin.splitter.expense.ExpenseService;
import com.tenzin.splitter.expense.SplitType;
import com.tenzin.splitter.group.GroupMemberRepository;
import com.tenzin.splitter.recurring.RecurringDtos.CreateRecurringExpenseRequest;
import com.tenzin.splitter.recurring.RecurringDtos.RecurringExpenseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RecurringExpenseService {

	private static final Logger log = LoggerFactory.getLogger(RecurringExpenseService.class);

	private final RecurringExpenseRepository recurring;

	private final GroupMemberRepository members;

	private final ExpenseService expenseService;

	private final TransactionTemplate transactions;

	private final Clock clock;

	public RecurringExpenseService(RecurringExpenseRepository recurring, GroupMemberRepository members,
			ExpenseService expenseService, TransactionTemplate transactions, Clock clock) {
		this.recurring = recurring;
		this.members = members;
		this.expenseService = expenseService;
		this.transactions = transactions;
		this.clock = clock;
	}

	@Transactional
	public RecurringExpenseResponse create(long groupId, long actorId, CreateRecurringExpenseRequest request) {
		if (!members.isMember(groupId, request.paidBy())) {
			throw ApiException.badRequest("The payer must be a member of this group");
		}
		RecurringExpense saved = recurring.save(new RecurringExpense(groupId, request.paidBy(),
				request.description().trim(), request.amountPaise(), request.dayOfMonth(), LocalDate.now(clock),
				actorId, clock.instant()));
		return RecurringExpenseResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<RecurringExpenseResponse> listActive(long groupId) {
		return recurring.findByGroupIdAndActiveTrueOrderByIdAsc(groupId)
			.stream()
			.map(RecurringExpenseResponse::from)
			.toList();
	}

	/** Stops future postings. Expenses already posted stay. Idempotent. */
	@Transactional
	public void stop(long groupId, long recurringId) {
		recurring.findByIdAndGroupId(recurringId, groupId)
			.orElseThrow(() -> ApiException.notFound("Recurring expense not found"))
			.stop();
	}

	/**
	 * Posts every active recurring expense whose next run date is today or earlier.
	 *
	 * Each one gets its own transaction, in which the expense is posted and next_run_date
	 * moves a month ahead together: if either fails, neither happens, and one bad row does
	 * not stop the others. If the job missed some days, each missed month is posted with its
	 * own date. Running twice on the same day posts nothing the second time.
	 *
	 * @return number of expenses posted
	 */
	public int postDue() {
		LocalDate today = LocalDate.now(clock);
		int posted = 0;
		for (Long id : recurring.findDueIds(today)) {
			try {
				Integer count = transactions.execute(status -> postIfDue(id, today));
				posted += count == null ? 0 : count;
			}
			catch (RuntimeException ex) {
				log.error("Could not post recurring expense {}", id, ex);
			}
		}
		if (posted > 0) {
			log.info("Posted {} recurring expense(s) for {}", posted, today);
		}
		return posted;
	}

	private int postIfDue(long id, LocalDate today) {
		RecurringExpense due = recurring.lockIfDue(id, today).orElse(null);
		if (due == null) {
			return 0; // stopped meanwhile, already posted, or another instance holds the lock
		}
		List<Long> participants = members.findUserIds(due.getGroupId());
		int count = 0;
		while (!due.getNextRunDate().isAfter(today)) {
			expenseService.create(due.getGroupId(), due.getCreatedBy(),
					new CreateExpenseRequest(due.getDescription(), due.getAmountPaise(), due.getPaidBy(),
							SplitType.EQUAL, due.getNextRunDate(), participants, null));
			due.advanceOneMonth();
			count++;
		}
		return count;
	}

}
