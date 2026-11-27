package com.tenzin.splitter.auth;

import com.tenzin.splitter.common.AppMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the reset email after the token is committed (never for a rolled-back request)
 * and off the request thread, so the 202 comes back equally fast whether or not the
 * email exists. A mail failure is logged; the user can simply ask again.
 */
@Component
public class PasswordResetMailer {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

	private final JavaMailSender mailSender;

	private final AppMailProperties properties;

	public PasswordResetMailer(JavaMailSender mailSender, AppMailProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onResetRequested(PasswordResetRequested event) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.from());
		message.setTo(event.email());
		message.setSubject("Reset your ExSplit password");
		message.setText("""
				Hi %s,

				Someone asked to reset the password for your ExSplit account. If it was you,
				use this reset token within %d minutes:

				%s

				Send it with your new password to:
				POST %s/api/auth/password-reset/confirm
				{"token": "<token>", "newPassword": "<new password>"}

				If you did not ask for this, ignore this email; your password stays the same.
				""".formatted(event.name(), event.validFor().toMinutes(), event.token(), properties.baseUrl()));
		try {
			mailSender.send(message);
		}
		catch (MailException ex) {
			log.error("Could not send password reset email", ex);
		}
	}

}
