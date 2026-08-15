package com.azizul.asenaki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AseNakiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AseNakiApplication.class, args);
    }
}
