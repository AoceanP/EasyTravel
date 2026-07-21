package com.easytravel.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * The Eureka service registry.
 *
 * <p>@EnableEurekaServer is the whole trick: it turns this ordinary Spring Boot app into
 * a running service registry — a live "phone book" that other services register with and
 * look each other up through. This module has no business logic; its only job is to BE
 * the registry.
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}