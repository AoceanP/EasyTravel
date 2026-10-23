package com.easytravel.web.ops;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * WHAT:   RabbitMQ's management HTTP API: how many messages wait in each queue.
 * WHY:    The queue depths (including the dead-letter queues) are the Ops
 *         room's view of the broker. Uses HTTP basic auth with RabbitMQ's
 *         local demo account unless configured otherwise.
 *
 * @author AoceanP
 */
@RegisterRestClient(configKey = "rabbitmq-admin")
@Path("/api")
@ClientHeaderParam(name = "Authorization", value = "{basicAuth}")
public interface RabbitAdminClient
{
    /**
     * WHAT:   One queue and how many messages wait in it.
     *
     * @param name     the queue name
     * @param messages messages waiting
     */
    record QueueInfo(String name, int messages)
    {
    }

    /**
     * WHAT:   GET /api/queues: every queue with its depth.
     * PRE:    columns limits the reply to the fields used
     * RETURN: the queues
     *
     * @param columns the fields to return
     * @return the queues
     */
    @GET
    @Path("/queues")
    @Produces(MediaType.APPLICATION_JSON)
    List<QueueInfo> queues(@QueryParam("columns") String columns);

    /**
     * WHAT:   The Authorization header value for every call.
     * WHY:    Read from configuration at call time, so the password is never in code.
     * PRE:    none
     * RETURN: "Basic <base64 of user:password>"
     *
     * @return the header value
     */
    default String basicAuth()
    {
        var config = ConfigProvider.getConfig();
        String user = config.getOptionalValue("rabbitmq.admin.username", String.class).orElse("guest");
        String pass = config.getOptionalValue("rabbitmq.admin.password", String.class).orElse("guest");
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }
}
