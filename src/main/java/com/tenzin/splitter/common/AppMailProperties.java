package com.tenzin.splitter.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param from    sender address for outgoing email
 * @param baseUrl public URL of this API, used to build links in emails
 */
@ConfigurationProperties("app.mail")
public record AppMailProperties(
		@DefaultValue("no-reply@exsplit.local") String from,
		@DefaultValue("http://localhost:8080") String baseUrl) {
}
