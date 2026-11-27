package com.tenzin.splitter.common;

/** One row of a {@code GROUP BY user} sum, in paise. */
public record UserAmount(Long userId, Long amountPaise) {
}
