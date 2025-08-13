package com.company.invoice.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.company.invoice")
@EnableAsync
@EnableScheduling
public class InvoiceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceApiApplication.class, args);
    }
}
