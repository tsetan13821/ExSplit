package com.tenzin.splitter.recurring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs daily at 00:05 server time. All the logic lives in the service so tests can call it. */
@Component
public class RecurringExpenseJob {

	private final RecurringExpenseService service;

	public RecurringExpenseJob(RecurringExpenseService service) {
		this.service = service;
	}

	@Scheduled(cron = "0 5 0 * * *")
	public int run() {
		return service.postDue();
	}

}
