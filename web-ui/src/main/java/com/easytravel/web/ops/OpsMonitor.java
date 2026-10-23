package com.easytravel.web.ops;

import com.easytravel.web.live.Messaging;
import com.easytravel.web.ops.RabbitAdminClient.QueueInfo;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WHAT:   Every 2 seconds, reads which services are up (Eureka), how many
 *         messages wait in each queue (RabbitMQ) and whether payments are
 *         paused, and tells open Ops rooms when anything changed.
 * WHY:    A background job, so a slow or dead dependency never slows the page:
 *         the page renders the last snapshot from memory, and a part that
 *         cannot be read shows as unknown instead of an error.
 *
 * @author AoceanP
 */
@ApplicationScoped
public class OpsMonitor
{
    /** WHAT: the logger. */
    private static final Logger LOG = Logger.getLogger(OpsMonitor.class.getName());

    /** WHAT: seconds between reads. */
    private static final long EVERY_SECONDS = 2;

    @Inject
    @RestClient
    EurekaClient eureka;

    @Inject
    @RestClient
    RabbitAdminClient rabbit;

    @Inject
    @RestClient
    PaymentsAdminClient payments;

    /** WHAT: the Ops room's push channel, shared by every open Ops room. */
    @Inject
    @Push(channel = Messaging.OPS_CHANNEL)
    PushContext push;

    @Resource
    ManagedScheduledExecutorService scheduler;

    /** WHAT: the latest snapshot; starts with everything unknown. */
    private volatile OpsSnapshot snapshot = OpsSnapshot.of(null, null, null);

    /** WHAT: the scheduled job, so it can be cancelled on shutdown. */
    private ScheduledFuture<?> job;

    /**
     * WHAT:   Starts the job once the web application has started.
     * PRE:    called by CDI when the application scope begins
     * POST:   a read runs every EVERY_SECONDS
     *
     * @param ignored the start-up event
     */
    void start(@Observes @Initialized(ApplicationScoped.class) Object ignored)
    {
        job = scheduler.scheduleWithFixedDelay(this::refresh, 1, EVERY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * WHAT:   The latest snapshot.
     * PRE:    none
     * RETURN: the snapshot (never null)
     *
     * @return the snapshot
     */
    public OpsSnapshot snapshot()
    {
        return snapshot;
    }

    /**
     * WHAT:   Reads everything now and notifies Ops rooms if it changed.
     * WHY:    Also called right after a control is used, so the page shows the
     *         effect at once instead of up to 2 seconds later.
     * PRE:    none
     * POST:   snapshot is current
     */
    public synchronized void refresh()
    {
        List<String> up = read(() -> Registry.upNames(eureka.apps()), "Eureka");
        List<QueueInfo> queues = read(() -> rabbit.queues("name,messages"), "RabbitMQ management API");
        Boolean paused = read(() -> payments.state().paused(), "payment-service demo controls");
        OpsSnapshot next = OpsSnapshot.of(up, queues, paused);
        if (!next.equals(snapshot))
        {
            snapshot = next;
            notifyOpsRooms(push);
        }
    }

    /**
     * WHAT:   Tells every open Ops room to re-render.
     * PRE:    channel is the Ops push channel
     * POST:   the message is sent, or the failure is logged
     *
     * @param channel the push channel
     */
    public static void notifyOpsRooms(PushContext channel)
    {
        try
        {
            channel.send(Messaging.PUSH_UPDATED);
        }
        catch (RuntimeException e)
        {
            LOG.log(Level.FINE, "No Ops room to notify", e);
        }
    }

    /**
     * WHAT:   Runs one read, turning any failure into "unknown".
     * PRE:    action is not null
     * RETURN: the value, or null if it could not be read
     *
     * @param action the read
     * @param what   what is being read, for the log
     * @param <T>    the value type
     * @return the value or null
     */
    private static <T> T read(Supplier<T> action, String what)
    {
        try
        {
            return action.get();
        }
        catch (RuntimeException e)
        {
            LOG.fine(() -> "Could not read " + what + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * WHAT:   Stops the job when the website stops.
     * PRE:    called by CDI on shutdown
     * POST:   no more reads are scheduled
     */
    @PreDestroy
    void stop()
    {
        if (job != null)
        {
            job.cancel(false);
        }
    }
}
