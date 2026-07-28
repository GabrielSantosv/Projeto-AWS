package com.marketflow.expedicao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExpedicaoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpedicaoServiceApplication.class, args);
    }
}
