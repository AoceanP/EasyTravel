package com.easytravel.web.gateway;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * WHAT:   The EasyTravel API gateway, as a typed Java interface.
 * WHY:    MicroProfile REST Client writes the HTTP code: each method is one
 *         endpoint. The base URL comes from configuration ("gateway/mp-rest/url",
 *         or the GATEWAY_MP_REST_URL environment variable in Docker). Its CDI
 *         scope is set in microprofile-config.properties ("gateway/mp-rest/scope").
 *
 * @author AoceanP
 */
@RegisterRestClient(configKey = "gateway")
@RegisterProvider(GatewayErrorMapper.class)
@Path("/api")
public interface GatewayClient
{
    /**
     * WHAT:   GET /api/flights/search: the flights on a route, best first.
     * PRE:    origin and destination are 3-letter codes
     * RETURN: the flights
     * THROWS: GatewayException if the gateway answers with an error
     *
     * @param origin      e.g. "LHR"
     * @param destination e.g. "JFK"
     * @return the flights
     */
    @GET
    @Path("/flights/search")
    @Produces(MediaType.APPLICATION_JSON)
    List<FlightDto> search(@QueryParam("origin") String origin, @QueryParam("destination") String destination);

    /**
     * WHAT:   POST /api/bookings: creates a PENDING booking and starts payment.
     * PRE:    request is valid
     * RETURN: the new booking
     * THROWS: GatewayException on 400, 404 or 503
     *
     * @param request the booking
     * @return the created booking
     */
    @POST
    @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    BookingDto book(BookingRequest request);

    /**
     * WHAT:   GET /api/bookings/{id}: one booking with its current status.
     * PRE:    id is a booking UUID
     * RETURN: the booking
     * THROWS: GatewayException on 404
     *
     * @param id the booking id
     * @return the booking
     */
    @GET
    @Path("/bookings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    BookingDto booking(@PathParam("id") String id);

    /**
     * WHAT:   GET /api/bookings?userId=: a customer's bookings, newest first.
     * PRE:    userId is not blank
     * RETURN: the bookings; empty if none
     * THROWS: GatewayException on error
     *
     * @param userId the customer
     * @return the bookings
     */
    @GET
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    List<BookingDto> bookings(@QueryParam("userId") String userId);

    /**
     * WHAT:   POST /api/bookings/{id}/cancel: starts cancelling and refunding a paid booking.
     * PRE:    id is a CONFIRMED booking
     * RETURN: the booking, now CANCELLING (202 Accepted)
     * THROWS: GatewayException on 404, or 409 if it cannot be cancelled now
     *
     * @param id the booking id
     * @return the booking
     */
    @POST
    @Path("/bookings/{id}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    BookingDto cancel(@PathParam("id") String id);
}
