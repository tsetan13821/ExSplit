package com.tenzin.splitter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 100) String name,
		// BCrypt only uses the first 72 bytes of a password
		@NotBlank @Size(min = 8, max = 72) String password) {
}
