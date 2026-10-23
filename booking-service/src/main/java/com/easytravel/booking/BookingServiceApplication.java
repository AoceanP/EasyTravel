package com.easytravel.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WHAT:   Starts the booking service: creates bookings and reacts to payment results.
 * WHY:    scanBasePackages = "com.easytravel" picks up the shared GlobalExceptionHandler.
 *
 * @author AoceanP
 */
@SpringBootApplication(scanBasePackages = "com.easytravel")
public class BookingServiceApplication
{
    /**
     * WHAT:   The program entry point.
     * PRE:    none
     *
     * @param args command-line arguments, passed on to Spring (e.g. --server.port=9000)
     */
    public static void main(String[] args)
    {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
