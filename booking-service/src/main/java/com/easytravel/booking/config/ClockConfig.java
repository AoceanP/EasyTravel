package com.easytravel.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * WHAT:   Provides the clock services read the time from.
 * WHY:    Injecting a Clock instead of calling Instant.now() directly lets
 *         tests use a fixed time, so timestamps can be asserted exactly.
 *
 * @author AoceanP
 */
@Configuration
public class ClockConfig
{
    /**
     * WHAT:   The real system clock, in UTC.
     * PRE:    none
     * RETURN: the clock
     *
     * @return the clock
     */
    @Bean
    public Clock clock()
    {
        return Clock.systemUTC();
    }
}
