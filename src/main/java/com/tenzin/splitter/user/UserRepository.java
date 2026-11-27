package com.tenzin.splitter.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

	/** Expects an already-normalised (trimmed, lowercased) email. */
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/**
	 * Adds one failed attempt. On reaching {@code max} the account locks until
	 * {@code lockUntil} and the counter starts again from zero, so the next lock needs
	 * another full run of failures. Postgres evaluates every SET expression against the
	 * old row, so both CASEs see the same {@code failed_attempts}.
	 */
	@Modifying
	@Query(value = """
			UPDATE users SET
			    failed_attempts = CASE WHEN failed_attempts + 1 >= :max THEN 0 ELSE failed_attempts + 1 END,
			    locked_until    = CASE WHEN failed_attempts + 1 >= :max THEN CAST(:lockUntil AS timestamptz)
			                           ELSE locked_until END
			WHERE id = :id
			""", nativeQuery = true)
	int recordFailedLogin(long id, int max, Instant lockUntil);

}
