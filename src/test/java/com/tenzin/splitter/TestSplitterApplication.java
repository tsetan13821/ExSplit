package com.tenzin.splitter;

import org.springframework.boot.SpringApplication;

public class TestSplitterApplication {

	public static void main(String[] args) {
		SpringApplication.from(SplitterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
