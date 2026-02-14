package com.saccos_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaccosSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaccosSystemApplication.class, args);
	}

}
