package com.tenzin.splitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Users authenticate with our own JWT flow, so the default in-memory user is not wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SplitterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SplitterApplication.class, args);
	}

}
