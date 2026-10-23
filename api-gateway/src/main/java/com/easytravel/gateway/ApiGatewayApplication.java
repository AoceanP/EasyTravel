package com.easytravel.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WHAT:   Starts the API gateway: the single entry point on port 8080.
 * WHY:    Clients only ever talk to the gateway; it routes /api/flights, /api/bookings
 *         and /api/payments to whichever instance Eureka lists (see application.yml).
 *
 * @author AoceanP
 */
@SpringBootApplication
public class ApiGatewayApplication
{
    /**
     * WHAT:   The program entry point.
     * PRE:    none
     *
     * @param args command-line arguments, passed on to Spring (e.g. --server.port=9000)
     */
    public static void main(String[] args)
    {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
