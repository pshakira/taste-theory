package com.tastetheory;

import org.springframework.boot.SpringApplication;

public class TestTasteTheoryApplication {

	public static void main(String[] args) {
		SpringApplication.from(TasteTheoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
