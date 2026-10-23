package com.easytravel.booking.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * WHAT:   Provides the HTTP client builders used by this service.
 * WHY:    Two builders are needed. The @LoadBalanced one resolves service
 *         NAMES ("http://inventory-service") through Eureka. The plain one is
 *         what the Eureka client itself uses to reach Eureka; if it were
 *         load-balanced too, Eureka would ask Eureka where Eureka is, and
 *         this service would never register.
 *
 * @author AoceanP
 */
@Configuration
public class RestClientConfig
{
    /**
     * WHAT:   A plain RestClient.Builder, the default for anything that does not ask for load balancing.
     * PRE:    none
     * RETURN: the builder
     *
     * @return the plain builder
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder()
    {
        return RestClient.builder();
    }

    /**
     * WHAT:   A RestClient.Builder that understands Eureka service names.
     * PRE:    a Eureka client is on the classpath
     * RETURN: the builder; inject it with @LoadBalanced
     *
     * @return the load-balanced builder
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder()
    {
        return RestClient.builder();
    }
}
