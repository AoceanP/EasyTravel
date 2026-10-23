package com.easytravel.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * WHAT:   Starts the Eureka service registry.
 * WHY:    @EnableEurekaServer turns this app into a live "phone book": every service
 *         registers here by name, so services find each other without hard-coded
 *         addresses. It has no business logic of its own.
 *
 * @author AoceanP
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication
{
    /**
     * WHAT:   The program entry point.
     * PRE:    none
     *
     * @param args command-line arguments, passed on to Spring (e.g. --server.port=9000)
     */
    public static void main(String[] args)
    {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
