package com.example.demo_sdd_spring.controller.api;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.service.GuestBookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestBookApiController.class)
class GuestBookApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GuestBookService guestBookService;

    private GuestBookEntry testEntry;

    @BeforeEach
    void setUp() {
        testEntry = new GuestBookEntry("John Doe", "john@example.com", "Test message");
        testEntry.setId(1L);
    }

    @Test
    void getAllEntries_ShouldReturnListOfEntries() throws Exception {
        // Given
        when(guestBookService.getAllEntries()).thenReturn(Arrays.asList(testEntry));

        // When & Then
        mockMvc.perform(get("/api/entries"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(guestBookService, times(1)).getAllEntries();
    }

    @Test
    void getEntryById_WhenExists_ShouldReturnEntry() throws Exception {
        // Given
        when(guestBookService.getEntryById(1L)).thenReturn(Optional.of(testEntry));

        // When & Then
        mockMvc.perform(get("/api/entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.message").value("Test message"));

        verify(guestBookService, times(1)).getEntryById(1L);
    }

    @Test
    void getEntryById_WhenNotExists_ShouldReturn404() throws Exception {
        // Given
        when(guestBookService.getEntryById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/entries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEntry_ShouldReturnCreatedEntry() throws Exception {
        // Given
        GuestBookEntry newEntry = new GuestBookEntry("Jane", "jane@example.com", "New message");
        when(guestBookService.createEntry(any(GuestBookEntry.class))).thenReturn(testEntry);

        // When & Then
        mockMvc.perform(post("/api/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEntry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(guestBookService, times(1)).createEntry(any(GuestBookEntry.class));
    }

    @Test
    void updateEntry_WhenExists_ShouldReturnUpdatedEntry() throws Exception {
        // Given
        GuestBookEntry updatedEntry = new GuestBookEntry("John Updated", "john.new@example.com", "Updated message");
        when(guestBookService.updateEntry(eq(1L), any(GuestBookEntry.class))).thenReturn(testEntry);

        // When & Then
        mockMvc.perform(put("/api/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedEntry)))
                .andExpect(status().isOk());

        verify(guestBookService, times(1)).updateEntry(eq(1L), any(GuestBookEntry.class));
    }

    @Test
    void updateEntry_WhenNotExists_ShouldReturn404() throws Exception {
        // Given
        GuestBookEntry updatedEntry = new GuestBookEntry("John", "john@example.com", "Message");
        when(guestBookService.updateEntry(eq(999L), any(GuestBookEntry.class)))
                .thenThrow(new RuntimeException("Not found"));

        // When & Then
        mockMvc.perform(put("/api/entries/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedEntry)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_WhenExists_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(guestBookService).deleteEntry(1L);

        // When & Then
        mockMvc.perform(delete("/api/entries/1"))
                .andExpect(status().isNoContent());

        verify(guestBookService, times(1)).deleteEntry(1L);
    }

    @Test
    void deleteEntry_WhenNotExists_ShouldReturn404() throws Exception {
        // Given
        doThrow(new RuntimeException("Not found")).when(guestBookService).deleteEntry(999L);

        // When & Then
        mockMvc.perform(delete("/api/entries/999"))
                .andExpect(status().isNotFound());
    }
}
