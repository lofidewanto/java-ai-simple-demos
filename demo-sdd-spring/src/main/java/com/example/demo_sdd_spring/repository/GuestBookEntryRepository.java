package com.example.demo_sdd_spring.repository;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestBookEntryRepository extends JpaRepository<GuestBookEntry, Long> {
    
    /**
     * Find all guest book entries ordered by creation date (newest first)
     * @return List of guest book entries
     */
    List<GuestBookEntry> findAllByOrderByCreatedAtDesc();
}
