package com.example.learn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class SalesSavvyProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesSavvyProjectApplication.class, args);
	}

}
