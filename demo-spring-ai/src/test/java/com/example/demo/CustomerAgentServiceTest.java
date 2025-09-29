package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerAgentServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAgentServiceTest.class);

    @Autowired
    CustomerAgentService customerAgentService;

    @Test
    void customer_response_creation() {
        String customerResponse = customerAgentService.generateCustomerResponse("CUST001",
                "What are your business hours?");

        logger.info("*** Generated Customer Response: " + customerResponse);

        assertNotNull(customerResponse);
        assertFalse(customerResponse.isEmpty());
        assertTrue(customerResponse.contains("Monday"));
        assertTrue(customerResponse.contains("Friday"));
    }
}