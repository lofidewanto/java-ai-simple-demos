package com.example.workflow.service;

import com.example.workflow.domain.TaskType;
import com.example.workflow.domain.WorkflowDefinition;
import com.example.workflow.domain.WorkflowHistoryEntry;
import com.example.workflow.domain.WorkflowInstance;
import com.example.workflow.dto.StateDto;
import com.example.workflow.dto.TransitionDto;
import com.example.workflow.dto.WorkflowInstanceResponse;
import com.example.workflow.exception.InstanceNotFoundException;
import com.example.workflow.exception.InvalidTransitionException;
import com.example.workflow.exception.WorkflowCompletedException;
import com.example.workflow.exception.WorkflowNotPausedException;
import com.example.workflow.exception.WorkflowNotRunningException;
import com.example.workflow.exception.WorkflowNotFoundException;
import com.example.workflow.exception.WorkflowPausedException;
import com.example.workflow.repository.WorkflowDefinitionRepository;
import com.example.workflow.repository.WorkflowHistoryEntryRepository;
import com.example.workflow.repository.WorkflowInstanceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkflowInstanceService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowHistoryEntryRepository historyRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    public WorkflowInstanceService(
            WorkflowInstanceRepository instanceRepository,
            WorkflowHistoryEntryRepository historyRepository,
            WorkflowDefinitionRepository definitionRepository,
            ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.historyRepository = historyRepository;
        this.definitionRepository = definitionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowInstanceResponse startInstance(String workflowName) {
        WorkflowDefinition definition = definitionRepository.findByName(workflowName)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowName));

        List<StateDto> states = deserialiseStates(definition);
        StateDto initialState = states.stream()
                .filter(StateDto::isInitial)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No initial state found in definition '" + workflowName + "'"));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowDefinition(definition);
        instance.setCurrentState(initialState.getName());
        instance.setStatus(STATUS_RUNNING);
        instance = instanceRepository.save(instance);

        WorkflowHistoryEntry entry = new WorkflowHistoryEntry();
        entry.setWorkflowInstance(instance);
        entry.setFromState(null);
        entry.setToState(initialState.getName());
        entry.setAction("START");
        entry.setTaskType(TaskType.HUMAN);
        historyRepository.save(entry);

        return toResponse(instance, List.of(entry));
    }

    @Transactional
    public WorkflowInstanceResponse triggerTransition(Long instanceId, String action) {
        WorkflowInstance instance = loadInstance(instanceId);

        if (STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new WorkflowCompletedException(instanceId, instance.getCurrentState());
        }

        if (STATUS_PAUSED.equals(instance.getStatus())) {
            throw new WorkflowPausedException(instanceId);
        }

        List<TransitionDto> transitions = deserialiseTransitions(instance.getWorkflowDefinition());
        TransitionDto transition = transitions.stream()
                .filter(t -> t.getFrom().equals(instance.getCurrentState())
                        && t.getAction().equals(action))
                .findFirst()
                .orElseThrow(() -> new InvalidTransitionException(action, instance.getCurrentState()));

        String previousState = instance.getCurrentState();
        instance.setCurrentState(transition.getTo());

        // Determine if the new state is terminal
        List<StateDto> states = deserialiseStates(instance.getWorkflowDefinition());
        boolean isTerminal = states.stream()
                .filter(s -> s.getName().equals(transition.getTo()))
                .findFirst()
                .map(StateDto::isTerminal)
                .orElse(false);

        if (isTerminal) {
            instance.setStatus(STATUS_COMPLETED);
        }

        WorkflowInstance savedInstance = instanceRepository.save(instance);

        WorkflowHistoryEntry entry = new WorkflowHistoryEntry();
        entry.setWorkflowInstance(savedInstance);
        entry.setFromState(previousState);
        entry.setToState(transition.getTo());
        entry.setAction(action);
        entry.setTaskType(transition.getTaskType() != null ? transition.getTaskType() : TaskType.HUMAN);
        historyRepository.save(entry);

        List<WorkflowHistoryEntry> history =
                historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(savedInstance);
        return toResponse(savedInstance, history);
    }

    @Transactional
    public WorkflowInstanceResponse pauseInstance(Long instanceId) {
        WorkflowInstance instance = loadInstance(instanceId);

        if (!STATUS_RUNNING.equals(instance.getStatus())) {
            throw new WorkflowNotRunningException(instanceId, instance.getStatus());
        }

        instance.setStatus(STATUS_PAUSED);
        WorkflowInstance savedInstance = instanceRepository.save(instance);

        List<WorkflowHistoryEntry> history =
                historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(savedInstance);
        return toResponse(savedInstance, history);
    }

    @Transactional
    public WorkflowInstanceResponse resumeInstance(Long instanceId) {
        WorkflowInstance instance = loadInstance(instanceId);

        if (!STATUS_PAUSED.equals(instance.getStatus())) {
            throw new WorkflowNotPausedException(instanceId, instance.getStatus());
        }

        instance.setStatus(STATUS_RUNNING);
        WorkflowInstance savedInstance = instanceRepository.save(instance);

        List<WorkflowHistoryEntry> history =
                historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(savedInstance);
        return toResponse(savedInstance, history);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse findById(Long id) {
        WorkflowInstance instance = loadInstance(id);
        List<WorkflowHistoryEntry> history =
                historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(instance);
        return toResponse(instance, history);
    }

    @Transactional(readOnly = true)
    public List<WorkflowInstanceResponse> findAll(String workflowName) {
        List<WorkflowInstance> instances;
        if (workflowName != null && !workflowName.isBlank()) {
            WorkflowDefinition definition = definitionRepository.findByName(workflowName)
                    .orElseThrow(() -> new WorkflowNotFoundException(workflowName));
            instances = instanceRepository.findByWorkflowDefinition(definition);
        } else {
            instances = instanceRepository.findAll();
        }
        return instances.stream()
                .map(inst -> {
                    List<WorkflowHistoryEntry> history =
                            historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(inst);
                    return toResponse(inst, history);
                })
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private WorkflowInstance loadInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));
    }

    private List<StateDto> deserialiseStates(WorkflowDefinition definition) {
        try {
            return objectMapper.readValue(definition.getStatesJson(),
                    new TypeReference<List<StateDto>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise states for '"
                    + definition.getName() + "'", e);
        }
    }

    private List<TransitionDto> deserialiseTransitions(WorkflowDefinition definition) {
        try {
            return objectMapper.readValue(definition.getTransitionsJson(),
                    new TypeReference<List<TransitionDto>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise transitions for '"
                    + definition.getName() + "'", e);
        }
    }

    private WorkflowInstanceResponse toResponse(WorkflowInstance instance,
                                                 List<WorkflowHistoryEntry> history) {
        WorkflowInstanceResponse response = new WorkflowInstanceResponse();
        response.setId(instance.getId());
        response.setWorkflowName(instance.getWorkflowDefinition().getName());
        response.setCurrentState(instance.getCurrentState());
        response.setStatus(instance.getStatus());
        response.setCreatedAt(instance.getCreatedAt());
        response.setUpdatedAt(instance.getUpdatedAt());
        response.setHistory(history.stream().map(this::toHistoryResponse).collect(Collectors.toList()));
        return response;
    }

    private WorkflowInstanceResponse.HistoryEntryResponse toHistoryResponse(WorkflowHistoryEntry entry) {
        WorkflowInstanceResponse.HistoryEntryResponse r = new WorkflowInstanceResponse.HistoryEntryResponse();
        r.setFromState(entry.getFromState());
        r.setToState(entry.getToState());
        r.setAction(entry.getAction());
        r.setTaskType(entry.getTaskType() != null ? entry.getTaskType().name() : null);
        r.setOccurredAt(entry.getOccurredAt());
        return r;
    }
}
