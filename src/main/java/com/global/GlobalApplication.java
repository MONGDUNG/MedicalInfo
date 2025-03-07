package com.global;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.global.map.service.MedicalFacilityService;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class GlobalApplication {

	private final MedicalFacilityService medicalFacilityService;

    public static void main(String[] args) {
        SpringApplication.run(GlobalApplication.class, args);
    }


}