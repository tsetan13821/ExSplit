package com.tenzin.splitter.recurring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

	@Query("select r.id from RecurringExpense r where r.active = true and r.nextRunDate <= :today order by r.id")
	List<Long> findDueIds(LocalDate today);

	/**
	 * {@code SELECT ... FOR UPDATE SKIP LOCKED} (lock timeout -2 is Hibernate's SKIP LOCKED).
	 * Re-checks that the row is still due under the lock, and if another app instance is
	 * already posting it, skips instead of waiting, so it can never be posted twice.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
	@Query("select r from RecurringExpense r where r.id = :id and r.active = true and r.nextRunDate <= :today")
	Optional<RecurringExpense> lockIfDue(Long id, LocalDate today);

	Optional<RecurringExpense> findByIdAndGroupId(Long id, Long groupId);

	List<RecurringExpense> findByGroupIdAndActiveTrueOrderByIdAsc(Long groupId);

}
