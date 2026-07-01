package com.moto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MotoApplication {
	public static void main(String[] args) {
		SpringApplication.run(MotoApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Bogota"));
	}
}
