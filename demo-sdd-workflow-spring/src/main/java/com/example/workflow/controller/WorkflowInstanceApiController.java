package com.example.workflow.controller;

import com.example.workflow.dto.StartWorkflowRequest;
import com.example.workflow.dto.TriggerTransitionRequest;
import com.example.workflow.dto.WorkflowInstanceResponse;
import com.example.workflow.service.WorkflowInstanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-instances")
public class WorkflowInstanceApiController {

    private final WorkflowInstanceService service;

    public WorkflowInstanceApiController(WorkflowInstanceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WorkflowInstanceResponse> start(
            @Valid @RequestBody StartWorkflowRequest request) {
        WorkflowInstanceResponse response = service.startInstance(request.getWorkflowName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowInstanceResponse>> listAll(
            @RequestParam(required = false) String workflowName) {
        return ResponseEntity.ok(service.findAll(workflowName));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowInstanceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/{id}/transitions")
    public ResponseEntity<WorkflowInstanceResponse> trigger(
            @PathVariable Long id,
            @Valid @RequestBody TriggerTransitionRequest request) {
        return ResponseEntity.ok(service.triggerTransition(id, request.getAction()));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<WorkflowInstanceResponse> pause(@PathVariable Long id) {
        return ResponseEntity.ok(service.pauseInstance(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<WorkflowInstanceResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(service.resumeInstance(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WorkflowInstanceResponse.HistoryEntryResponse>> history(
            @PathVariable Long id) {
        WorkflowInstanceResponse response = service.findById(id);
        return ResponseEntity.ok(response.getHistory());
    }
}
