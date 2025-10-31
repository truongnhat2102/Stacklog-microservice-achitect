package com.stacklog.class_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {"com.stacklog.class_service", "com.stacklog.core_service"})
@EnableJpaRepositories(basePackages = "com.stacklog.class_service.model.repo")
@EntityScan(basePackages = "com.stacklog.class_service.model.entities")
public class ClassServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassServiceApplication.class, args);
	}

}
