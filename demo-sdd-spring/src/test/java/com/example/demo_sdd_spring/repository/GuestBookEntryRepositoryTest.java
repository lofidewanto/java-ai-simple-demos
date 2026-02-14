package com.example.demo_sdd_spring.repository;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GuestBookEntryRepositoryTest {

    @Autowired
    private GuestBookEntryRepository repository;

    @Test
    void saveEntry_ShouldPersistEntry() {
        // Given
        GuestBookEntry entry = new GuestBookEntry("Test User", "test@example.com", "Test message");

        // When
        GuestBookEntry saved = repository.save(entry);

        // Then
        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findById_WhenExists_ShouldReturnEntry() {
        // Given
        GuestBookEntry entry = repository.save(new GuestBookEntry("John", "john@example.com", "Message"));

        // When
        Optional<GuestBookEntry> found = repository.findById(entry.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<GuestBookEntry> found = repository.findById(999L);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ShouldReturnEntriesInDescendingOrder() throws InterruptedException {
        // Given
        GuestBookEntry entry1 = repository.save(new GuestBookEntry("First", "first@example.com", "First message"));
        Thread.sleep(100); // Ensure different timestamps
        GuestBookEntry entry2 = repository.save(new GuestBookEntry("Second", "second@example.com", "Second message"));

        // When
        List<GuestBookEntry> entries = repository.findAllByOrderByCreatedAtDesc();

        // Then
        assertEquals(2, entries.size());
        assertEquals("Second", entries.get(0).getName()); // Most recent first
        assertEquals("First", entries.get(1).getName());
    }

    @Test
    void deleteById_ShouldRemoveEntry() {
        // Given
        GuestBookEntry entry = repository.save(new GuestBookEntry("To Delete", "delete@example.com", "Message"));
        Long id = entry.getId();

        // When
        repository.deleteById(id);

        // Then
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void updateEntry_ShouldModifyExistingEntry() {
        // Given
        GuestBookEntry entry = repository.save(new GuestBookEntry("Original", "original@example.com", "Original message"));
        Long id = entry.getId();

        // When
        entry.setName("Updated Name");
        entry.setMessage("Updated message");
        repository.save(entry);

        // Then
        Optional<GuestBookEntry> updated = repository.findById(id);
        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
        assertEquals("Updated message", updated.get().getMessage());
    }
}
