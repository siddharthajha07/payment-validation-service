package com.rbc.paymentvalidation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Payment Validation Service.
 *
 * <p>{@code @SpringBootApplication} enables component scanning from this package
 * downwards, so every class annotated as a component, service, repository or
 * controller under {@code com.rbc.paymentvalidation} is discovered automatically.
 */
@SpringBootApplication
public class PaymentValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentValidationServiceApplication.class, args);
    }
}
