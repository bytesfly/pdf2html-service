package com.github.iflyendless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Pdf2HtmlService {

    public static void main(String[] args) {
        SpringApplication.run(Pdf2HtmlService.class);
    }
}
