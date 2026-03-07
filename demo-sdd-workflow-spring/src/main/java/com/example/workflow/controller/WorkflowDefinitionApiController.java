package com.example.workflow.controller;

import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.service.WorkflowDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-definitions")
public class WorkflowDefinitionApiController {

    private final WorkflowDefinitionService service;

    public WorkflowDefinitionApiController(WorkflowDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionDto>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<WorkflowDefinitionDto> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.findByName(name));
    }
}
