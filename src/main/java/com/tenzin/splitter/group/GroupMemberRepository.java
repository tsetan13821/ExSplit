package com.tenzin.splitter.group;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

	@Query("select count(m) > 0 from GroupMember m where m.id.groupId = :groupId and m.id.userId = :userId")
	boolean isMember(Long groupId, Long userId);

	@Query("""
			select count(m) > 0 from GroupMember m
			where m.id.groupId = :groupId and m.id.userId = :userId and m.role = :role
			""")
	boolean hasRole(Long groupId, Long userId, GroupRole role);

	@Query("""
			select m from GroupMember m join fetch m.user
			where m.id.groupId = :groupId
			order by m.joinedAt, m.id.userId
			""")
	List<GroupMember> findMembersWithUsers(Long groupId);

	/** How many of {@code userIds} belong to the group, for checking a whole list at once. */
	@Query("select count(m) from GroupMember m where m.id.groupId = :groupId and m.id.userId in :userIds")
	long countMembersAmong(Long groupId, Collection<Long> userIds);

}
