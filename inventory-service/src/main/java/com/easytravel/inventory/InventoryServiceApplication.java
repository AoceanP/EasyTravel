package com.easytravel.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WHAT:   Starts the inventory service: flight search and price quotes.
 * WHY:    scanBasePackages = "com.easytravel" widens the component scan so this
 *         service also picks up the shared GlobalExceptionHandler in common.
 *
 * @author AoceanP
 */
@SpringBootApplication(scanBasePackages = "com.easytravel")
public class InventoryServiceApplication
{
    /**
     * WHAT:   The program entry point.
     * PRE:    none
     *
     * @param args command-line arguments, passed on to Spring (e.g. --server.port=9000)
     */
    public static void main(String[] args)
    {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
