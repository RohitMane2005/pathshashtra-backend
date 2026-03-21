package com.pathshashtra.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PathshashtraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PathshashtraBackendApplication.class, args);
    }
}
