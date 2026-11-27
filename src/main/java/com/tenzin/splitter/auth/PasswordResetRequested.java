package com.tenzin.splitter.auth;

import java.time.Duration;

/** Published inside the request transaction; the email goes out only after it commits. */
public record PasswordResetRequested(String email, String name, String token, Duration validFor) {
}
