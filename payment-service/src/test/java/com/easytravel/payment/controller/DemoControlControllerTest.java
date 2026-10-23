package com.easytravel.payment.controller;

import com.easytravel.payment.messaging.BookingCreatedListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * WHAT:   Tests for the demo pause/resume switch.
 *
 * @author AoceanP
 */
class DemoControlControllerTest
{
    private MessageListenerContainer container;
    private MockMvc mvc;

    /**
     * WHAT:   Wires the controller with a fake listener container.
     * POST:   mvc is ready
     */
    @BeforeEach
    void setUp()
    {
        container = mock(MessageListenerContainer.class);
        RabbitListenerEndpointRegistry registry = mock(RabbitListenerEndpointRegistry.class);
        when(registry.getListenerContainer(BookingCreatedListener.LISTENER_ID)).thenReturn(container);
        mvc = MockMvcBuilders.standaloneSetup(new DemoControlController(registry)).build();
    }

    /**
     * WHAT:   Pause, then resume.
     * WHY:    Each stops or starts the booking-created listener and reports the state.
     * RETURN: nothing; fails if the listener is not driven
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if a request cannot be performed
     */
    @Test
    void pauseAndResume() throws Exception
    {
        when(container.isRunning()).thenReturn(false);
        mvc.perform(post("/admin/payments/pause")).andExpect(jsonPath("$.paused").value(true));
        verify(container).stop();

        when(container.isRunning()).thenReturn(true);
        mvc.perform(post("/admin/payments/resume")).andExpect(jsonPath("$.paused").value(false));
        verify(container).start();
        mvc.perform(get("/admin/payments")).andExpect(jsonPath("$.paused").value(false));
    }
}
