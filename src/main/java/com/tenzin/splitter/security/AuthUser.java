package com.tenzin.splitter.security;

/**
 * The authenticated caller, rebuilt from the access token on every request.
 * Holds identity only: group membership is always checked against the database.
 */
public record AuthUser(long id, String email) {
}
