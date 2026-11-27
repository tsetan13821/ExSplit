package com.tenzin.splitter.group;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

	Optional<GroupInvite> findByTokenHash(String tokenHash);

	/**
	 * Claims the invite in one statement. Returns 0 if it is expired or already used. When
	 * two people accept at once, Postgres row-locks the invite: the second UPDATE waits,
	 * re-checks {@code usedAt is null} after the first commits, and matches nothing.
	 */
	@Modifying
	@Query("""
			update GroupInvite i set i.usedAt = :now, i.usedBy = :userId
			where i.id = :id and i.usedAt is null and i.expiresAt > :now
			""")
	int claim(Long id, Long userId, Instant now);

}
