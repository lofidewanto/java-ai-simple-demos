package com.example.demo_sdd_spring.controller.web;

import com.example.demo_sdd_spring.model.GuestBookEntry;
import com.example.demo_sdd_spring.service.GuestBookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class GuestBookWebController {

    private final GuestBookService guestBookService;

    public GuestBookWebController(GuestBookService guestBookService) {
        this.guestBookService = guestBookService;
    }

    @GetMapping("/")
    public String redirectToEntries() {
        return "redirect:/entries";
    }

    @GetMapping("/entries")
    public String listEntries(Model model) {
        List<GuestBookEntry> entries = guestBookService.getAllEntries();
        model.addAttribute("entries", entries);
        return "entries/list";
    }

    @GetMapping("/entries/new")
    public String showAddForm(Model model) {
        model.addAttribute("entry", new GuestBookEntry());
        model.addAttribute("isEdit", false);
        return "entries/form";
    }

    @PostMapping("/entries")
    public String createEntry(@ModelAttribute GuestBookEntry entry, RedirectAttributes redirectAttributes) {
        guestBookService.createEntry(entry);
        redirectAttributes.addFlashAttribute("successMessage", "Entry added successfully!");
        return "redirect:/entries";
    }

    @GetMapping("/entries/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return guestBookService.getEntryById(id)
                .map(entry -> {
                    model.addAttribute("entry", entry);
                    model.addAttribute("isEdit", true);
                    return "entries/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
                    return "redirect:/entries";
                });
    }

    @PostMapping("/entries/{id}")
    public String updateEntry(@PathVariable Long id, @ModelAttribute GuestBookEntry entry, 
                             RedirectAttributes redirectAttributes) {
        try {
            guestBookService.updateEntry(id, entry);
            redirectAttributes.addFlashAttribute("successMessage", "Entry updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
        }
        return "redirect:/entries";
    }

    @PostMapping("/entries/{id}/delete")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            guestBookService.deleteEntry(id);
            redirectAttributes.addFlashAttribute("successMessage", "Entry deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
        }
        return "redirect:/entries";
    }
}
