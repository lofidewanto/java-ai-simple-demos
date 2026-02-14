package com.example.demo_sdd_spring.controller.api;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.service.GuestBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entries")
@Tag(name = "Guest Book", description = "Guest Book Entry Management API")
public class GuestBookApiController {

    private final GuestBookService guestBookService;

    public GuestBookApiController(GuestBookService guestBookService) {
        this.guestBookService = guestBookService;
    }

    @Operation(
        summary = "Get all guest book entries",
        description = "Retrieves all guest book entries ordered by newest first"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved entries")
    })
    @GetMapping
    public List<GuestBookEntry> getAllEntries() {
        return guestBookService.getAllEntries();
    }

    @Operation(
        summary = "Get a specific entry by ID",
        description = "Retrieves a single guest book entry by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entry found"),
        @ApiResponse(responseCode = "404", description = "Entry not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<GuestBookEntry> getEntryById(
        @Parameter(description = "ID of the entry to retrieve", required = true) 
        @PathVariable Long id
    ) {
        return guestBookService.getEntryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new guest book entry",
        description = "Adds a new entry to the guest book"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Entry created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<GuestBookEntry> createEntry(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Guest book entry to create",
            required = true
        )
        @RequestBody GuestBookEntry entry
    ) {
        GuestBookEntry createdEntry = guestBookService.createEntry(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
    }

    @Operation(
        summary = "Update an existing entry",
        description = "Updates an existing guest book entry by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entry updated successfully"),
        @ApiResponse(responseCode = "404", description = "Entry not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<GuestBookEntry> updateEntry(
        @Parameter(description = "ID of the entry to update", required = true) 
        @PathVariable Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated entry data",
            required = true
        )
        @RequestBody GuestBookEntry entry
    ) {
        try {
            GuestBookEntry updatedEntry = guestBookService.updateEntry(id, entry);
            return ResponseEntity.ok(updatedEntry);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete an entry",
        description = "Removes a guest book entry by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entry deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Entry not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
        @Parameter(description = "ID of the entry to delete", required = true) 
        @PathVariable Long id
    ) {
        try {
            guestBookService.deleteEntry(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
