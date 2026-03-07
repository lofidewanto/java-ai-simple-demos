package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public class TriggerTransitionRequest {

    @NotBlank(message = "action must not be blank")
    private String action;

    public TriggerTransitionRequest() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
