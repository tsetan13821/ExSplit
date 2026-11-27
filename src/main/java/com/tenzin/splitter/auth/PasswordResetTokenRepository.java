package com.tenzin.splitter.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	/**
	 * Uses the token in one statement: 0 rows means expired or already used, and two
	 * concurrent confirms cannot both succeed.
	 */
	@Modifying
	@Query("""
			update PasswordResetToken t set t.usedAt = :now
			where t.id = :id and t.usedAt is null and t.expiresAt > :now
			""")
	int claim(Long id, Instant now);

	/** After a successful reset, any other links already sent stop working too. */
	@Modifying
	@Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
	int invalidateAllForUser(Long userId, Instant now);

}
