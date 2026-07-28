package com.marketflow.fiscal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FiscalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiscalServiceApplication.class, args);
    }
}
