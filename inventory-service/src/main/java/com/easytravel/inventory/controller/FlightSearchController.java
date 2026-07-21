package com.easytravel.inventory.controller;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * HTTP edge. @RestController serializes return values straight to JSON. Stays thin:
 * parse the request, delegate to the service, no business logic here.
 * Example: GET /api/flights/search?origin=LHR&destination=JFK
 */
@RestController
public class FlightSearchController {

    private final InventoryService inventoryService;

    public FlightSearchController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/api/flights/search")
    public List<FlightDTO> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination) {
        return inventoryService.findBestFlights(origin, destination);
    }
}