package com.dosmartie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.dosmartie"})
public class BynitGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(BynitGatewayApplication.class, args);
	}

}
