package com.example.workflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Thymeleaf @Controller that serves thin HTML page shells for the Workflow Engine UI.
 * All workflow data is loaded client-side via JavaScript fetch() calls to the existing
 * /api/* REST endpoints. No model attributes containing workflow data are passed to templates.
 */
@Controller
@RequestMapping("/ui")
public class WorkflowUiController {

    /** Entry point — redirect to the workflow definitions page. */
    @GetMapping
    public String index() {
        return "redirect:/ui/workflows";
    }

    /** Workflow definitions list page (AC-1, AC-2). */
    @GetMapping("/workflows")
    public String workflows() {
        return "workflows";
    }

    /** Workflow instances list page (AC-3). */
    @GetMapping("/instances")
    public String instances() {
        return "instances";
    }

    /** Instance detail page — history + available actions + pause/resume (AC-4, AC-5). */
    @GetMapping("/instances/{id}")
    public String instanceDetail(@PathVariable Long id,
                                 org.springframework.ui.Model model) {
        model.addAttribute("instanceId", id);
        return "instance-detail";
    }
}
