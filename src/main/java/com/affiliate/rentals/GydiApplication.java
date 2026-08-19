package com.affiliate.rentals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class
}, scanBasePackages = "com.affiliate.rentals.gydi")
@EnableScheduling
public class GydiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GydiApplication.class, args);
	}

}
