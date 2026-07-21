package com.easytravel.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point. scanBasePackages = "com.easytravel" so the shared
 * GlobalExceptionHandler in common is discovered (same reasoning as inventory-service).
 */
@SpringBootApplication(scanBasePackages = "com.easytravel")
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}