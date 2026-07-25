package com.backend.observerr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ObserverrApplication {

	public static void main(String[] args) {
		SpringApplication.run(ObserverrApplication.class, args);
	}

}
