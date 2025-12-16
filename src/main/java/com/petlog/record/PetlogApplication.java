package com.petlog.record;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients
@SpringBootApplication
public class PetlogApplication {

	public static void main(String[] args) {


        SpringApplication.run(PetlogApplication.class, args);
	}


}
