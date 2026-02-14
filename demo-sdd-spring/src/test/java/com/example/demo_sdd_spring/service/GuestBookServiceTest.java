package com.example.demo_sdd_spring.service;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.repository.GuestBookEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestBookServiceTest {

    @Mock
    private GuestBookEntryRepository repository;

    @InjectMocks
    private GuestBookService service;

    private GuestBookEntry testEntry;

    @BeforeEach
    void setUp() {
        testEntry = new GuestBookEntry("John Doe", "john@example.com", "Test message");
        testEntry.setId(1L);
    }

    @Test
    void getAllEntries_ShouldReturnAllEntries() {
        // Given
        List<GuestBookEntry> entries = Arrays.asList(testEntry, new GuestBookEntry("Jane", "jane@example.com", "Message"));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(entries);

        // When
        List<GuestBookEntry> result = service.getAllEntries();

        // Then
        assertEquals(2, result.size());
        verify(repository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getEntryById_WhenExists_ShouldReturnEntry() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testEntry));

        // When
        Optional<GuestBookEntry> result = service.getEntryById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getName());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void getEntryById_WhenNotExists_ShouldReturnEmpty() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<GuestBookEntry> result = service.getEntryById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void createEntry_ShouldSaveAndReturnEntry() {
        // Given
        when(repository.save(any(GuestBookEntry.class))).thenReturn(testEntry);

        // When
        GuestBookEntry result = service.createEntry(testEntry);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(repository, times(1)).save(testEntry);
    }

    @Test
    void updateEntry_WhenExists_ShouldUpdateAndReturn() {
        // Given
        GuestBookEntry updatedData = new GuestBookEntry("John Updated", "john.new@example.com", "Updated message");
        when(repository.findById(1L)).thenReturn(Optional.of(testEntry));
        when(repository.save(any(GuestBookEntry.class))).thenReturn(testEntry);

        // When
        GuestBookEntry result = service.updateEntry(1L, updatedData);

        // Then
        assertNotNull(result);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(testEntry);
    }

    @Test
    void updateEntry_WhenNotExists_ShouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> service.updateEntry(999L, testEntry));
    }

    @Test
    void deleteEntry_WhenExists_ShouldDelete() {
        // Given
        when(repository.existsById(1L)).thenReturn(true);

        // When
        service.deleteEntry(1L);

        // Then
        verify(repository, times(1)).existsById(1L);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void deleteEntry_WhenNotExists_ShouldThrowException() {
        // Given
        when(repository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> service.deleteEntry(999L));
    }
}
