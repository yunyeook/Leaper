package com.ssafy.spark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SparkApplication {

	public static void main(String[] args) {
		SpringApplication.run(SparkApplication.class, args);
	}

}
