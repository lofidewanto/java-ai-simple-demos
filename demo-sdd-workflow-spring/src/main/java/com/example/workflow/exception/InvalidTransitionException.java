package com.example.workflow.exception;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String action, String state) {
        super("No transition found for action '" + action + "' in state '" + state + "'");
    }
}
