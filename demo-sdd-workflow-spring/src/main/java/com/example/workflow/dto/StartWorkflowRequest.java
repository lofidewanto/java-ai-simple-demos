package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public class StartWorkflowRequest {

    @NotBlank(message = "workflowName must not be blank")
    private String workflowName;

    public StartWorkflowRequest() {
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }
}
