package com.tenzin.splitter.expense;

import java.util.List;
import java.util.Optional;

import com.tenzin.splitter.common.UserAmount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	Page<Expense> findByGroupId(Long groupId, Pageable pageable);

	/** Scoped to the group, so an id from another group is "not found" rather than deletable. */
	Optional<Expense> findByIdAndGroupId(Long id, Long groupId);

	/** Total each member has paid for, summed in the database. */
	@Query("""
			select new com.tenzin.splitter.common.UserAmount(e.paidBy, sum(e.amountPaise))
			from Expense e where e.groupId = :groupId
			group by e.paidBy
			""")
	List<UserAmount> sumPaidByUser(Long groupId);

	/** Total of each member's own shares, summed in the database. */
	@Query("""
			select new com.tenzin.splitter.common.UserAmount(s.userId, sum(s.sharePaise))
			from Expense e join e.shares s where e.groupId = :groupId
			group by s.userId
			""")
	List<UserAmount> sumSharesByUser(Long groupId);

}
