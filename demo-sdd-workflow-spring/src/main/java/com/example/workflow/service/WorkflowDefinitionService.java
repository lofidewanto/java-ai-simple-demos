package com.example.workflow.service;

import com.example.workflow.domain.WorkflowDefinition;
import com.example.workflow.dto.StateDto;
import com.example.workflow.dto.TransitionDto;
import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.exception.WorkflowNotFoundException;
import com.example.workflow.repository.WorkflowDefinitionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionService(WorkflowDefinitionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void loadFromDto(WorkflowDefinitionDto dto) {
        // Idempotent: skip if already loaded
        if (repository.existsByName(dto.getName())) {
            return;
        }

        validate(dto);

        try {
            WorkflowDefinition entity = new WorkflowDefinition();
            entity.setName(dto.getName());
            entity.setDescription(dto.getDescription());
            entity.setSource(dto.getSource());
            entity.setStatesJson(objectMapper.writeValueAsString(dto.getStates()));
            entity.setTransitionsJson(objectMapper.writeValueAsString(dto.getTransitions()));
            repository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist workflow definition '" + dto.getName() + "'", e);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionDto findByName(String name) {
        WorkflowDefinition entity = repository.findByName(name)
                .orElseThrow(() -> new WorkflowNotFoundException(name));
        return toDto(entity);
    }

    // ---- DSL Validation ----

    private void validate(WorkflowDefinitionDto dto) {
        List<StateDto> states = dto.getStates();
        List<TransitionDto> transitions = dto.getTransitions();

        long initialCount = states.stream().filter(StateDto::isInitial).count();
        if (initialCount != 1) {
            throw new IllegalArgumentException("Workflow must have exactly one initial state");
        }

        long terminalCount = states.stream().filter(StateDto::isTerminal).count();
        if (terminalCount < 1) {
            throw new IllegalArgumentException("Workflow must have at least one terminal state");
        }

        Set<String> stateNames = states.stream().map(StateDto::getName).collect(Collectors.toSet());

        for (TransitionDto t : transitions) {
            if (!stateNames.contains(t.getFrom())) {
                throw new IllegalArgumentException(
                        "Transition 'from' references unknown state: " + t.getFrom());
            }
            if (!stateNames.contains(t.getTo())) {
                throw new IllegalArgumentException(
                        "Transition 'to' references unknown state: " + t.getTo());
            }
        }

        // Check for duplicate (from, action) pairs
        Set<String> seen = new java.util.HashSet<>();
        for (TransitionDto t : transitions) {
            String key = t.getFrom() + ":" + t.getAction();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate transition: from=" + t.getFrom() + ", action=" + t.getAction());
            }
        }
    }

    // ---- Entity → DTO mapping ----

    WorkflowDefinitionDto toDto(WorkflowDefinition entity) {
        try {
            WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setSource(entity.getSource());
            dto.setStates(objectMapper.readValue(entity.getStatesJson(),
                    new TypeReference<List<StateDto>>() {}));
            dto.setTransitions(objectMapper.readValue(entity.getTransitionsJson(),
                    new TypeReference<List<TransitionDto>>() {}));
            return dto;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise workflow definition '" + entity.getName() + "'", e);
        }
    }
}
