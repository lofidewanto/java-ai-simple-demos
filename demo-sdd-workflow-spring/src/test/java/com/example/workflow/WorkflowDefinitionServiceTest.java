package com.example.workflow;

import com.example.workflow.domain.WorkflowDefinition;
import com.example.workflow.dto.StateDto;
import com.example.workflow.dto.TransitionDto;
import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.exception.WorkflowNotFoundException;
import com.example.workflow.repository.WorkflowDefinitionRepository;
import com.example.workflow.service.WorkflowDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TC-07 through TC-09
 */
@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceTest {

    @Mock
    private WorkflowDefinitionRepository repository;

    private WorkflowDefinitionService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new WorkflowDefinitionService(repository, objectMapper);
    }

    // TC-07: findAll() returns all persisted definitions
    @Test
    void tc07_findAllReturnsTwoDefinitions() throws Exception {
        WorkflowDefinition def1 = buildEntity("wf-one");
        WorkflowDefinition def2 = buildEntity("wf-two");
        when(repository.findAll()).thenReturn(List.of(def1, def2));

        List<WorkflowDefinitionDto> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(WorkflowDefinitionDto::getName)
                .containsExactlyInAnyOrder("wf-one", "wf-two");
    }

    // TC-08: findByName returns correct definition
    @Test
    void tc08_findByNameReturnsCorrectDefinition() throws Exception {
        WorkflowDefinition def = buildEntity("order-processing");
        when(repository.findByName("order-processing")).thenReturn(Optional.of(def));

        WorkflowDefinitionDto dto = service.findByName("order-processing");

        assertThat(dto.getName()).isEqualTo("order-processing");
        assertThat(dto.getStates()).hasSize(1);
        assertThat(dto.getTransitions()).hasSize(1);
    }

    // TC-09: findByName throws WorkflowNotFoundException for unknown name
    @Test
    void tc09_findByNameThrowsForUnknownName() {
        when(repository.findByName("does-not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByName("does-not-exist"))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessageContaining("does-not-exist");
    }

    // Helper

    private WorkflowDefinition buildEntity(String name) throws Exception {
        StateDto state = new StateDto();
        state.setName("START");
        state.setInitial(true);
        state.setTerminal(false);

        TransitionDto transition = new TransitionDto();
        transition.setFrom("START");
        transition.setTo("END");
        transition.setAction("GO");

        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setName(name);
        entity.setDescription("test");
        entity.setStatesJson(objectMapper.writeValueAsString(List.of(state)));
        entity.setTransitionsJson(objectMapper.writeValueAsString(List.of(transition)));
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
