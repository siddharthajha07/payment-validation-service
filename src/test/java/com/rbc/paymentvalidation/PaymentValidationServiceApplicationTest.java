package com.rbc.paymentvalidation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the Spring application context starts successfully.
 *
 * This is deliberately the first test in the project: almost every misconfiguration
 * — a missing dependency, an unresolvable bean, a malformed application.yml —
 * surfaces here as a startup failure rather than later as a confusing runtime error.
 */
@SpringBootTest
class PaymentValidationServiceApplicationTest {

    @Test
    void contextLoads() {
        // The assertion is the successful startup of the context itself: if any bean
        // cannot be created, @SpringBootTest fails before this method body is reached.
    }
}
