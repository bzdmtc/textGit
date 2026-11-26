package com.example.smart_learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLearningApplication.class, args);
    }

}
