package com.example.demo.config;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(OrderService orderService) {
        return args -> {
            // Order 1: fully processed — SHIPPED
            Order o1 = orderService.createOrder(
                new Order("Alice Johnson", "Wireless Headphones", 2));
            orderService.transitionOrder(o1.getId(), "CHECK_INVENTORY");
            orderService.transitionOrder(o1.getId(), "MARK_AVAILABLE");
            orderService.transitionOrder(o1.getId(), "SHIP");

            // Order 2: item unavailable — CUSTOMER_NOTIFIED
            Order o2 = orderService.createOrder(
                new Order("Bob Smith", "Mechanical Keyboard", 1));
            orderService.transitionOrder(o2.getId(), "CHECK_INVENTORY");
            orderService.transitionOrder(o2.getId(), "MARK_UNAVAILABLE");

            // Order 3: freshly submitted — SUBMITTED
            orderService.createOrder(
                new Order("Carol Williams", "USB-C Hub", 3));
        };
    }
}
