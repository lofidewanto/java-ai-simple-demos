package com.example.workflow.dto;

import java.util.List;

public class WorkflowDefinitionDto {

    private String name;
    private String description;
    private String source;
    private List<StateDto> states;
    private List<TransitionDto> transitions;

    public WorkflowDefinitionDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<StateDto> getStates() {
        return states;
    }

    public void setStates(List<StateDto> states) {
        this.states = states;
    }

    public List<TransitionDto> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<TransitionDto> transitions) {
        this.transitions = transitions;
    }
}
