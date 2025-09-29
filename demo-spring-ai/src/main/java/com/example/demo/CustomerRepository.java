package com.example.demo;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    @Tool(description = "Get the customer details by customer ID")
    public Customer getCustomerById(String customerId) {
        // In a real application, this would fetch data from a database
        // Here, we return a mock customer for demonstration purposes
        return new Customer(
                "CUST001",
                "Jane Doe",
                "jane.doe@example.com",
                "+1-555-1234",
                "English",
                LocalDateTime.now().minusDays(5),
                "Premium");
    }
}
