package com.easytravel.web.ops;

import com.easytravel.web.ops.RabbitAdminClient.QueueInfo;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for reading Eureka's reply and building the Ops snapshot.
 *
 * @author AoceanP
 */
class OpsSnapshotTest
{
    /**
     * WHAT:   Eureka's reply in its real shape, with one service DOWN.
     * WHY:    Only applications with an instance UP count; a lone application
     *         may arrive as an object instead of an array.
     * RETURN: nothing; fails if the names are wrong
     */
    @Test
    void readsEurekaReply()
    {
        String json = """
                {"applications":{"application":[
                  {"name":"BOOKING-SERVICE","instance":[{"status":"UP"}]},
                  {"name":"PAYMENT-SERVICE","instance":{"status":"DOWN"}},
                  {"name":"API-GATEWAY","instance":[{"status":"DOWN"},{"status":"UP"}]}]}}""";
        JsonObject apps = jakarta.json.Json.createReader(new java.io.StringReader(json)).readObject();
        assertThat(Registry.upNames(apps)).containsExactly("BOOKING-SERVICE", "API-GATEWAY");
    }

    /**
     * WHAT:   A snapshot with payments paused and one service missing.
     * WHY:    payment-service shows "paused", a missing one "down", and the
     *         queues come in display order with their dead-letter queues.
     * RETURN: nothing; fails if states or queue order are wrong
     */
    @Test
    void buildsSnapshot()
    {
        OpsSnapshot s = OpsSnapshot.of(List.of("API-GATEWAY", "BOOKING-SERVICE", "PAYMENT-SERVICE"),
                List.of(new QueueInfo("payment.booking-created.queue", 3), new QueueInfo("amq.gen-xyz", 0)), true);
        Map<String, String> states = s.services().stream()
                .collect(java.util.stream.Collectors.toMap(OpsSnapshot.Service::name, OpsSnapshot.Service::state));
        assertThat(states).containsEntry("payment-service", "paused")
                .containsEntry("inventory-service", "down")
                .containsEntry("discovery-server", "up")
                .containsEntry("rabbitmq", "up");
        assertThat(s.queues()).hasSize(8);
        assertThat(s.queues().getFirst()).isEqualTo(new QueueInfo("payment.booking-created.queue", 3));
        assertThat(s.queues().get(1).name()).isEqualTo("payment.booking-created.queue.dlq");
    }

    /**
     * WHAT:   Nothing could be read.
     * WHY:    Services show "unknown"/"down" and queues are null, never an error.
     * RETURN: nothing; fails if it throws or invents data
     */
    @Test
    void nothingReadable()
    {
        OpsSnapshot s = OpsSnapshot.of(null, null, null);
        assertThat(s.queues()).isNull();
        assertThat(s.services()).extracting(OpsSnapshot.Service::state).contains("unknown", "down", "up");
    }
}
