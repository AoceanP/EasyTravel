package com.easytravel.web.ops;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * WHAT:   The Eureka registry's REST API.
 * WHY:    Answers "which services are registered and UP right now", the same
 *         list the Eureka dashboard shows. Read as raw JSON because Eureka's
 *         format uses keys like "$" that do not map well onto records.
 *
 * @author AoceanP
 */
@RegisterRestClient(configKey = "eureka")
@Path("/eureka")
public interface EurekaClient
{
    /**
     * WHAT:   GET /eureka/apps: every registered application and instance.
     * RETURN: the registry as JSON
     *
     * @return the registry
     */
    @GET
    @Path("/apps")
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject apps();
}
