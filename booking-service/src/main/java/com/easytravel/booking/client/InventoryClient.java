package com.easytravel.booking.client;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.common.exception.ResourceNotFoundException;
import com.easytravel.common.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * WHAT:   Calls inventory-service to get a flight's real price.
 * WHY:    The base URL is "http://inventory-service": a NAME, not a host.
 *         The @LoadBalanced RestClient.Builder (see RestClientConfig) asks
 *         Eureka where that service is running, so no address is hard-coded
 *         and several instances share the load.
 *
 * @author AoceanP
 */
@Component
public class InventoryClient
{
    /** WHAT: the HTTP client, already pointed at inventory-service. */
    private final RestClient restClient;

    /**
     * WHAT:   Creates the client.
     * PRE:    builder is load-balanced; baseUrl names a Eureka service
     *
     * @param builder the load-balanced builder from RestClientConfig
     * @param baseUrl the inventory service's base URL (default http://inventory-service)
     */
    public InventoryClient(@LoadBalanced RestClient.Builder builder,
                           @Value("${easytravel.inventory.base-url:http://inventory-service}") String baseUrl)
    {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * WHAT:   Fetches one flight with its current base fare.
     * WHY:    Turns HTTP failures into the project's own exceptions, so the
     *         client gets a clear 404 or 503 instead of a 500.
     * PRE:    the arguments are not null
     * RETURN: the flight
     * THROWS: ResourceNotFoundException if inventory says the flight does not exist;
     *         ServiceUnavailableException if inventory cannot be reached
     *
     * @param flightNumber the flight, e.g. "BA249"
     * @param origin       departure airport
     * @param destination  arrival airport
     * @return the flight
     * @throws ResourceNotFoundException   if there is no such flight
     * @throws ServiceUnavailableException if inventory-service is down
     */
    public FlightDTO quote(String flightNumber, String origin, String destination)
    {
        try
        {
            FlightDTO flight = restClient.get()
                    .uri(uri -> uri.path("/api/flights/quote")
                            .queryParam("flightNumber", flightNumber)
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .build())
                    .retrieve()
                    .body(FlightDTO.class);
            if (flight == null)
            {
                throw new ServiceUnavailableException("inventory-service returned an empty quote", null);
            }
            return flight;
        }
        catch (HttpClientErrorException e)
        {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND)
            {
                throw new ResourceNotFoundException(
                        "flight " + flightNumber + " from " + origin + " to " + destination + " not found");
            }
            throw new ServiceUnavailableException("inventory-service rejected the quote request", e);
        }
        catch (RestClientException | IllegalStateException e)
        {
            // IllegalStateException: the load balancer found no running inventory-service instance
            throw new ServiceUnavailableException("inventory-service is unavailable, try again shortly", e);
        }
    }
}
