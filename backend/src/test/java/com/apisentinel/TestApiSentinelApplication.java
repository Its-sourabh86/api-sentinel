package com.apisentinel;

import org.springframework.boot.SpringApplication;

public class TestApiSentinelApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiSentinelApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
