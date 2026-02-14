package com.example.demo_sdd_spring.config;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.repository.GuestBookEntryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(GuestBookEntryRepository repository) {
        return args -> {
            // Add sample entries for demonstration
            repository.save(new GuestBookEntry(
                "Alice Johnson", 
                "alice@example.com", 
                "Great website! Really enjoyed browsing through the content. Keep up the good work!"
            ));
            
            repository.save(new GuestBookEntry(
                "Bob Smith", 
                "bob.smith@example.com", 
                "This is exactly what I was looking for. The design is clean and intuitive."
            ));
            
            repository.save(new GuestBookEntry(
                "Carol Davis", 
                null, 
                "Wonderful experience! I'll definitely be coming back. Thanks for creating this!"
            ));
            
            System.out.println("Sample data initialized: 3 guest book entries added.");
        };
    }
}
