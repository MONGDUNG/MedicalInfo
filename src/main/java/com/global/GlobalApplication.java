package com.global;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
@EnableScheduling
public class GlobalApplication {



    public static void main(String[] args) {
        SpringApplication.run(GlobalApplication.class, args);
    }


}