package com.rbc.paymentvalidation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Payment Validation Service.
 *
 * SpringBootApplication scans this package downwards, so every component, service, repository
 * and controller under com.rbc.paymentvalidation is found automatically.
 * ConfigurationPropertiesScan does the same for the properties classes.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PaymentValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentValidationServiceApplication.class, args);
    }
}
