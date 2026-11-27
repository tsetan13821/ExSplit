package com.tenzin.splitter.settlement;

import java.util.List;

import com.tenzin.splitter.common.UserAmount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	@Query("""
			select new com.tenzin.splitter.common.UserAmount(s.fromUser, sum(s.amountPaise))
			from Settlement s where s.groupId = :groupId
			group by s.fromUser
			""")
	List<UserAmount> sumSentByUser(Long groupId);

	@Query("""
			select new com.tenzin.splitter.common.UserAmount(s.toUser, sum(s.amountPaise))
			from Settlement s where s.groupId = :groupId
			group by s.toUser
			""")
	List<UserAmount> sumReceivedByUser(Long groupId);

}
