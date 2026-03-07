package com.example.workflow.dto;

import com.example.workflow.domain.TaskType;

public class TransitionDto {

    private String from;
    private String to;
    private String action;
    private TaskType taskType = TaskType.HUMAN;
    private String description;

    public TransitionDto() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = (taskType != null) ? taskType : TaskType.HUMAN;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
