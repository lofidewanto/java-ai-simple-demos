package com.example.demo_sdd_spring.controller.web;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.service.GuestBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestBookWebController.class)
class GuestBookWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GuestBookService guestBookService;

    private GuestBookEntry testEntry;

    @BeforeEach
    void setUp() {
        testEntry = new GuestBookEntry("John Doe", "john@example.com", "Test message");
        testEntry.setId(1L);
    }

    @Test
    void redirectToEntries_ShouldRedirectToEntriesPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));
    }

    @Test
    void listEntries_ShouldShowEntriesList() throws Exception {
        // Given
        when(guestBookService.getAllEntries()).thenReturn(Arrays.asList(testEntry));

        // When & Then
        mockMvc.perform(get("/entries"))
                .andExpect(status().isOk())
                .andExpect(view().name("entries/list"))
                .andExpect(model().attributeExists("entries"));

        verify(guestBookService, times(1)).getAllEntries();
    }

    @Test
    void showAddForm_ShouldShowForm() throws Exception {
        mockMvc.perform(get("/entries/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("entries/form"))
                .andExpect(model().attributeExists("entry"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    void createEntry_ShouldRedirectToList() throws Exception {
        // Given
        when(guestBookService.createEntry(any(GuestBookEntry.class))).thenReturn(testEntry);

        // When & Then
        mockMvc.perform(post("/entries")
                        .param("name", "John Doe")
                        .param("email", "john@example.com")
                        .param("message", "Test message"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));

        verify(guestBookService, times(1)).createEntry(any(GuestBookEntry.class));
    }

    @Test
    void showEditForm_WhenExists_ShouldShowForm() throws Exception {
        // Given
        when(guestBookService.getEntryById(1L)).thenReturn(Optional.of(testEntry));

        // When & Then
        mockMvc.perform(get("/entries/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("entries/form"))
                .andExpect(model().attributeExists("entry"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    void showEditForm_WhenNotExists_ShouldRedirectWithError() throws Exception {
        // Given
        when(guestBookService.getEntryById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/entries/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));
    }

    @Test
    void updateEntry_WhenExists_ShouldRedirectToList() throws Exception {
        // Given
        when(guestBookService.updateEntry(eq(1L), any(GuestBookEntry.class))).thenReturn(testEntry);

        // When & Then
        mockMvc.perform(post("/entries/1")
                        .param("name", "John Updated")
                        .param("email", "john.new@example.com")
                        .param("message", "Updated message"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));

        verify(guestBookService, times(1)).updateEntry(eq(1L), any(GuestBookEntry.class));
    }

    @Test
    void deleteEntry_WhenExists_ShouldRedirectToList() throws Exception {
        // Given
        doNothing().when(guestBookService).deleteEntry(1L);

        // When & Then
        mockMvc.perform(post("/entries/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));

        verify(guestBookService, times(1)).deleteEntry(1L);
    }

    @Test
    void deleteEntry_WhenNotExists_ShouldRedirectWithError() throws Exception {
        // Given
        doThrow(new RuntimeException("Not found")).when(guestBookService).deleteEntry(999L);

        // When & Then
        mockMvc.perform(post("/entries/999/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entries"));
    }
}
