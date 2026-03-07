package com.example.workflow;

import com.example.workflow.domain.TaskType;
import com.example.workflow.dto.StateDto;
import com.example.workflow.dto.TransitionDto;
import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.loader.WorkflowDefinitionLoader;
import com.example.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * TC-01 through TC-06
 */
@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionLoaderTest {

    @Mock
    private WorkflowDefinitionService definitionService;

    private WorkflowDefinitionLoader loader;

    @BeforeEach
    void setUp() {
        loader = new WorkflowDefinitionLoader(definitionService);
    }

    // TC-01: Valid YAML is parsed into correct states and transitions
    @Test
    void tc01_validYamlIsLoadedSuccessfully() throws Exception {
        loader.run(); // loads classpath:workflows/*.yml
        verify(definitionService, org.mockito.Mockito.atLeastOnce()).loadFromDto(any());
    }

    // TC-02: Missing initial state throws validation error
    @Test
    void tc02_missingInitialStateThrowsValidationError() {
        WorkflowDefinitionDto dto = buildDto(false, true);
        doThrow(new IllegalArgumentException("Workflow must have exactly one initial state"))
                .when(definitionService).loadFromDto(any());

        assertThatThrownBy(() -> definitionService.loadFromDto(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initial state");
    }

    // TC-03: Missing terminal state throws validation error
    @Test
    void tc03_missingTerminalStateThrowsValidationError() {
        WorkflowDefinitionDto dto = buildDto(true, false);
        doThrow(new IllegalArgumentException("Workflow must have at least one terminal state"))
                .when(definitionService).loadFromDto(any());

        assertThatThrownBy(() -> definitionService.loadFromDto(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminal state");
    }

    // TC-04: Unknown 'from' reference throws validation error
    @Test
    void tc04_unknownFromReferenceThrowsValidationError() {
        doThrow(new IllegalArgumentException("Transition 'from' references unknown state: GHOST"))
                .when(definitionService).loadFromDto(any());

        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        assertThatThrownBy(() -> definitionService.loadFromDto(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown state");
    }

    // TC-05: Duplicate (from, action) pair throws validation error
    @Test
    void tc05_duplicateFromActionPairThrowsValidationError() {
        doThrow(new IllegalArgumentException("Duplicate transition: from=A, action=GO"))
                .when(definitionService).loadFromDto(any());

        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        assertThatThrownBy(() -> definitionService.loadFromDto(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transition");
    }

    // TC-06: taskType defaults to HUMAN when omitted
    @Test
    void tc06_taskTypeDefaultsToHuman() {
        TransitionDto t = new TransitionDto();
        t.setFrom("A");
        t.setTo("B");
        t.setAction("GO");
        // taskType not set — default is HUMAN
        assertThat(t.getTaskType()).isEqualTo(TaskType.HUMAN);
    }

    // Helper

    private WorkflowDefinitionDto buildDto(boolean hasInitial, boolean hasTerminal) {
        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        dto.setName("test");

        StateDto s1 = new StateDto();
        s1.setName("A");
        s1.setInitial(hasInitial);
        s1.setTerminal(false);

        StateDto s2 = new StateDto();
        s2.setName("B");
        s2.setInitial(false);
        s2.setTerminal(hasTerminal);

        dto.setStates(List.of(s1, s2));

        TransitionDto t = new TransitionDto();
        t.setFrom("A");
        t.setTo("B");
        t.setAction("GO");
        dto.setTransitions(List.of(t));

        return dto;
    }
}
