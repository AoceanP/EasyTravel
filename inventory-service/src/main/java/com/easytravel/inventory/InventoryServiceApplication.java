package com.easytravel.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * Boot entry point. scanBasePackages = "com.easytravel" widens the component scan
 * so this service also discovers the shared GlobalExceptionHandler in common.
 */
@SpringBootApplication(scanBasePackages = "com.easytravel")
public class InventoryServiceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}