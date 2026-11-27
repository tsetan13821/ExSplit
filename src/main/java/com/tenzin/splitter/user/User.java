package com.tenzin.splitter.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String name;

	@Setter
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Setter
	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Setter
	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public User(String email, String name, String passwordHash, Instant createdAt) {
		this.email = email;
		this.name = name;
		this.passwordHash = passwordHash;
		this.createdAt = createdAt;
	}

}
