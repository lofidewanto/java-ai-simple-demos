package com.example.workflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("WORKFLOW_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InstanceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInstanceNotFound(InstanceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("INSTANCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("INVALID_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowCompleted(WorkflowCompletedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("WORKFLOW_COMPLETED", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowNotRunningException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotRunning(WorkflowNotRunningException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("WORKFLOW_NOT_RUNNING", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowPausedException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowPaused(WorkflowPausedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("WORKFLOW_PAUSED", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowNotPausedException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotPaused(WorkflowNotPausedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("WORKFLOW_NOT_PAUSED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("VALIDATION_ERROR", message));
    }

    private Map<String, Object> errorBody(String error, String message) {
        return Map.of(
                "error", error,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
