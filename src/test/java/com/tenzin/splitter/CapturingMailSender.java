package com.tenzin.splitter;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Stands in for the SMTP server in tests: records messages instead of sending them.
 * Mail goes out on another thread, so tests wait for it with {@link #awaitMessageTo}.
 */
public class CapturingMailSender extends JavaMailSenderImpl {

	private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

	@Override
	public void send(SimpleMailMessage... messages) {
		sent.addAll(Arrays.asList(messages));
	}

	public List<SimpleMailMessage> messagesTo(String address) {
		return sent.stream().filter(m -> m.getTo() != null && Arrays.asList(m.getTo()).contains(address)).toList();
	}

	public SimpleMailMessage awaitMessageTo(String address, Duration timeout) throws InterruptedException {
		Instant deadline = Instant.now().plus(timeout);
		while (Instant.now().isBefore(deadline)) {
			Optional<SimpleMailMessage> found = messagesTo(address).stream().findFirst();
			if (found.isPresent()) {
				return found.get();
			}
			Thread.sleep(20);
		}
		throw new AssertionError("No email to " + address + " within " + timeout);
	}

}
