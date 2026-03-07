package com.example.workflow.dto;

import java.time.LocalDateTime;
import java.util.List;

public class WorkflowInstanceResponse {

    private Long id;
    private String workflowName;
    private String currentState;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<HistoryEntryResponse> history;

    public WorkflowInstanceResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<HistoryEntryResponse> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryEntryResponse> history) {
        this.history = history;
    }

    // ----- Nested response DTO for history entries -----

    public static class HistoryEntryResponse {

        private String fromState;
        private String toState;
        private String action;
        private String taskType;
        private LocalDateTime occurredAt;

        public HistoryEntryResponse() {
        }

        public String getFromState() {
            return fromState;
        }

        public void setFromState(String fromState) {
            this.fromState = fromState;
        }

        public String getToState() {
            return toState;
        }

        public void setToState(String toState) {
            this.toState = toState;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }
    }
}
