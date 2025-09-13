package com.creedpetitt.aiservicesbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiBackendApplication.class, args);
    }

}
