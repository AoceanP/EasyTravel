package com.easytravel.web.ops;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * WHAT:   payment-service's demo pause switch, called directly (not through the gateway).
 * WHY:    The switch's /admin paths are not routed by the gateway on purpose;
 *         only containers inside the Docker network, like this website, reach it.
 *
 * @author AoceanP
 */
@RegisterRestClient(configKey = "payments")
@Path("/admin/payments")
@Produces(MediaType.APPLICATION_JSON)
public interface PaymentsAdminClient
{
    /**
     * WHAT:   Whether payments are paused.
     *
     * @param paused true while payment-service is not charging
     */
    record PauseState(boolean paused)
    {
    }

    /**
     * WHAT:   Reads the switch.
     * RETURN: the state
     *
     * @return the state
     */
    @GET
    PauseState state();

    /**
     * WHAT:   Pauses payments.
     * RETURN: the new state
     *
     * @return the state
     */
    @POST
    @Path("/pause")
    PauseState pause();

    /**
     * WHAT:   Resumes payments.
     * RETURN: the new state
     *
     * @return the state
     */
    @POST
    @Path("/resume")
    PauseState resume();
}
