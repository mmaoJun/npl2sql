package com.nlp2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Nlp2SqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nlp2SqlApplication.class, args);
    }
}
