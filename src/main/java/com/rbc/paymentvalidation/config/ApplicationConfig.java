package com.rbc.paymentvalidation.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide beans that are not components in their own right.
 */
@Configuration
public class ApplicationConfig {

    /**
     * The source of the current time for every component that needs one.
     *
     * <p>Exposing the clock as a bean rather than letting classes call {@code now()}
     * directly is what makes time-dependent rules testable. A test can supply a fixed
     * clock and assert precisely what happens on a given date, instead of computing dates
     * relative to the real one and quietly changing meaning as the calendar moves.
     *
     * <p>UTC rather than the system default deliberately: settlement dates are scheme
     * dates, and a service whose behaviour depends on the timezone of the machine it
     * happens to run on is a service that behaves differently in each environment.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
