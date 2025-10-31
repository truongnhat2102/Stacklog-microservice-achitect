package com.stacklog.document_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {"com.stacklog.document_service", "com.stacklog.core_service"})
@EnableJpaRepositories(basePackages = "com.stacklog.document_service.model.repo")
@EntityScan(basePackages = "com.stacklog.document_service.model.entities")
@EnableFeignClients
public class DocumentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentServiceApplication.class, args);
	}

}
