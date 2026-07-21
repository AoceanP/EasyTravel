package com.easytravel.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point. scanBasePackages = "com.easytravel" so the shared
 * GlobalExceptionHandler in common is discovered (same reasoning as the other services).
 */
@SpringBootApplication(scanBasePackages = "com.easytravel")
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}