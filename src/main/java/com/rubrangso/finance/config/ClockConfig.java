package com.rubrangso.finance.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a {@link Clock} bean so that all date-sensitive business logic receives
 * the current time via injection rather than calling {@code LocalDate.now()} directly.
 * Tests can override this bean with a fixed clock to get deterministic behaviour.
 */
@Configuration
public class ClockConfig {

    /** Returns the system default-zone clock for use in production and development. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
