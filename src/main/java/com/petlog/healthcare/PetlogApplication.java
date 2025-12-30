package com.petlog.healthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Healthcare AI Chatbot Service (포트: 8085)
 * MSA PetLog 프로젝트 핵심 서비스
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.petlog.healthcare.repository")
@EnableKafka
public class PetlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetlogApplication.class, args);
    }
}
