package com.example.demo_sdd_spring.service;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.repository.GuestBookEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GuestBookService {

    private final GuestBookEntryRepository repository;

    public GuestBookService(GuestBookEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GuestBookEntry> getAllEntries() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<GuestBookEntry> getEntryById(Long id) {
        return repository.findById(id);
    }

    public GuestBookEntry createEntry(GuestBookEntry entry) {
        return repository.save(entry);
    }

    public GuestBookEntry updateEntry(Long id, GuestBookEntry entry) {
        GuestBookEntry existingEntry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guest book entry not found with id: " + id));
        
        existingEntry.setName(entry.getName());
        existingEntry.setEmail(entry.getEmail());
        existingEntry.setMessage(entry.getMessage());
        
        return repository.save(existingEntry);
    }

    public void deleteEntry(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Guest book entry not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
