package com.tenzin.splitter.recurring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FirstRunDateTest {

	private static LocalDate date(String iso) {
		return LocalDate.parse(iso);
	}

	@Test
	void laterThisMonth() {
		assertThat(RecurringExpense.firstRunDate(date("2026-03-10"), 15)).isEqualTo(date("2026-03-15"));
	}

	@Test
	void todayCounts() {
		assertThat(RecurringExpense.firstRunDate(date("2026-03-10"), 10)).isEqualTo(date("2026-03-10"));
	}

	@Test
	void alreadyPassedMovesToNextMonth() {
		assertThat(RecurringExpense.firstRunDate(date("2026-03-10"), 1)).isEqualTo(date("2026-04-01"));
	}

	@Test
	void rollsOverTheYear() {
		assertThat(RecurringExpense.firstRunDate(date("2026-12-31"), 28)).isEqualTo(date("2027-01-28"));
	}

	@Test
	void day28WorksInFebruary() {
		assertThat(RecurringExpense.firstRunDate(date("2027-02-01"), 28)).isEqualTo(date("2027-02-28"));
	}

}
