package com.tenzin.splitter.group;

import java.util.List;

import com.tenzin.splitter.group.GroupDtos.GroupSummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

	@Query("""
			select new com.tenzin.splitter.group.GroupDtos$GroupSummary(g.id, g.name, m.role, g.createdAt)
			from GroupMember m join ExpenseGroup g on g.id = m.id.groupId
			where m.id.userId = :userId
			order by g.createdAt desc, g.id desc
			""")
	List<GroupSummary> findSummariesForUser(Long userId);

}
