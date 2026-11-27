package com.tenzin.splitter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {

	private PasswordResetDtos() {
	}

	public record ResetRequest(@NotBlank @Email @Size(max = 255) String email) {
	}

	public record ResetConfirmRequest(
			@NotBlank String token,
			// BCrypt only uses the first 72 bytes of a password
			@NotBlank @Size(min = 8, max = 72) String newPassword) {
	}

}
