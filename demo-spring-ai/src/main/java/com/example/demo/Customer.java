package com.example.demo;

import java.time.LocalDateTime;

public record Customer(
        String id,
        String name,
        String email,
        String phoneNumber,
        String preferredLanguage,
        LocalDateTime lastInteraction,
        String customerSegment) {

    public Customer(String id, String name, String email, String phoneNumber, 
                   String preferredLanguage, String customerSegment) {
        this(id, name, email, phoneNumber, preferredLanguage, LocalDateTime.now(), customerSegment);
    }

    public static Customer withLastInteraction(String id, String name, String email, 
                                              String phoneNumber, String preferredLanguage, 
                                              String customerSegment, LocalDateTime lastInteraction) {
        return new Customer(id, name, email, phoneNumber, preferredLanguage, lastInteraction, customerSegment);
    }

    public Customer withUpdatedLastInteraction(LocalDateTime newLastInteraction) {
        return new Customer(id, name, email, phoneNumber, preferredLanguage, newLastInteraction, customerSegment);
    }
}