package com.example.workflow;

import com.example.workflow.domain.TaskType;
import com.example.workflow.domain.WorkflowDefinition;
import com.example.workflow.domain.WorkflowHistoryEntry;
import com.example.workflow.domain.WorkflowInstance;
import com.example.workflow.dto.WorkflowInstanceResponse;
import com.example.workflow.exception.InstanceNotFoundException;
import com.example.workflow.exception.InvalidTransitionException;
import com.example.workflow.exception.WorkflowCompletedException;
import com.example.workflow.exception.WorkflowNotPausedException;
import com.example.workflow.exception.WorkflowNotRunningException;
import com.example.workflow.exception.WorkflowPausedException;
import com.example.workflow.repository.WorkflowDefinitionRepository;
import com.example.workflow.repository.WorkflowHistoryEntryRepository;
import com.example.workflow.repository.WorkflowInstanceRepository;
import com.example.workflow.service.WorkflowInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TC-10 through TC-23
 */
@ExtendWith(MockitoExtension.class)
class WorkflowInstanceServiceTest {

    @Mock private WorkflowInstanceRepository instanceRepository;
    @Mock private WorkflowHistoryEntryRepository historyRepository;
    @Mock private WorkflowDefinitionRepository definitionRepository;

    private WorkflowInstanceService service;
    private ObjectMapper objectMapper;

    // A minimal order-processing definition JSON
    private static final String STATES_JSON = """
            [
              {"name":"NEW","initial":true,"terminal":false},
              {"name":"CHECKING_AVAILABILITY","initial":false,"terminal":false},
              {"name":"PAYMENT_PENDING","initial":false,"terminal":false},
              {"name":"SHIPPED","initial":false,"terminal":true},
              {"name":"CANCELLED","initial":false,"terminal":true}
            ]""";

    private static final String TRANSITIONS_JSON = """
            [
              {"from":"NEW","to":"CHECKING_AVAILABILITY","action":"SUBMIT_ORDER","taskType":"HUMAN"},
              {"from":"CHECKING_AVAILABILITY","to":"PAYMENT_PENDING","action":"STOCK_AVAILABLE","taskType":"GATEWAY"},
              {"from":"CHECKING_AVAILABILITY","to":"CANCELLED","action":"STOCK_UNAVAILABLE","taskType":"GATEWAY"},
              {"from":"PAYMENT_PENDING","to":"SHIPPED","action":"PAYMENT_COLLECTED","taskType":"SERVICE"}
            ]""";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new WorkflowInstanceService(instanceRepository, historyRepository,
                definitionRepository, objectMapper);
    }

    // TC-10: startInstance creates instance in initial state NEW, status RUNNING
    @Test
    void tc10_startInstanceCreatesInInitialState() {
        WorkflowDefinition def = definition();
        when(definitionRepository.findByName("order-processing")).thenReturn(Optional.of(def));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            wi.setId(1L);
            wi.setCreatedAt(LocalDateTime.now());
            wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstanceResponse response = service.startInstance("order-processing");

        assertThat(response.getCurrentState()).isEqualTo("NEW");
        assertThat(response.getStatus()).isEqualTo("RUNNING");
    }

    // TC-11: startInstance writes initial history entry with action START
    @Test
    void tc11_startInstanceWritesInitialHistoryEntry() {
        WorkflowDefinition def = definition();
        when(definitionRepository.findByName("order-processing")).thenReturn(Optional.of(def));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            wi.setId(1L);
            wi.setCreatedAt(LocalDateTime.now());
            wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        ArgumentCaptor<WorkflowHistoryEntry> captor =
                ArgumentCaptor.forClass(WorkflowHistoryEntry.class);
        when(historyRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.startInstance("order-processing");

        WorkflowHistoryEntry entry = captor.getValue();
        assertThat(entry.getFromState()).isNull();
        assertThat(entry.getToState()).isEqualTo("NEW");
        assertThat(entry.getAction()).isEqualTo("START");
        assertThat(entry.getTaskType()).isEqualTo(TaskType.HUMAN);
    }

    // TC-12: triggerTransition moves instance to correct next state
    @Test
    void tc12_triggerTransitionMovesToCorrectState() {
        WorkflowInstance instance = runningInstance("NEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            if (wi.getUpdatedAt() == null) wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(any()))
                .thenReturn(List.of());

        WorkflowInstanceResponse response = service.triggerTransition(1L, "SUBMIT_ORDER");

        assertThat(response.getCurrentState()).isEqualTo("CHECKING_AVAILABILITY");
    }

    // TC-13: triggerTransition writes history entry with correct taskType GATEWAY
    @Test
    void tc13_triggerTransitionWritesHistoryWithCorrectTaskType() {
        WorkflowInstance instance = runningInstance("CHECKING_AVAILABILITY");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            if (wi.getUpdatedAt() == null) wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        ArgumentCaptor<WorkflowHistoryEntry> captor =
                ArgumentCaptor.forClass(WorkflowHistoryEntry.class);
        when(historyRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(any()))
                .thenReturn(List.of());

        service.triggerTransition(1L, "STOCK_AVAILABLE");

        assertThat(captor.getValue().getTaskType()).isEqualTo(TaskType.GATEWAY);
    }

    // TC-14: triggerTransition on COMPLETED instance throws WorkflowCompletedException
    @Test
    void tc14_triggerOnCompletedInstanceThrows() {
        WorkflowInstance instance = instanceWithStatus("SHIPPED", "COMPLETED");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.triggerTransition(1L, "SUBMIT_ORDER"))
                .isInstanceOf(WorkflowCompletedException.class);
    }

    // TC-15: triggerTransition with unknown action throws InvalidTransitionException
    @Test
    void tc15_unknownActionThrowsInvalidTransitionException() {
        WorkflowInstance instance = runningInstance("NEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.triggerTransition(1L, "UNKNOWN_ACTION"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("UNKNOWN_ACTION");
    }

    // TC-16: triggerTransition with valid action in wrong state throws InvalidTransitionException
    @Test
    void tc16_validActionInWrongStateThrowsInvalidTransitionException() {
        WorkflowInstance instance = runningInstance("NEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.triggerTransition(1L, "STOCK_AVAILABLE"))
                .isInstanceOf(InvalidTransitionException.class);
    }

    // TC-17: Reaching terminal state sets status COMPLETED
    @Test
    void tc17_terminalStateSetStatusCompleted() {
        WorkflowInstance instance = runningInstance("PAYMENT_PENDING");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            if (wi.getUpdatedAt() == null) wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(any()))
                .thenReturn(List.of());

        WorkflowInstanceResponse response = service.triggerTransition(1L, "PAYMENT_COLLECTED");

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCurrentState()).isEqualTo("SHIPPED");
    }

    // TC-18: findById throws InstanceNotFoundException for unknown ID
    @Test
    void tc18_findByIdThrowsForUnknownId() {
        when(instanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(InstanceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // TC-19: pauseInstance sets status to PAUSED for running instance
    @Test
    void tc19_pauseInstanceSetsStatusPaused() {
        WorkflowInstance instance = runningInstance("NEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            if (wi.getUpdatedAt() == null) wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        when(historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(any()))
                .thenReturn(List.of());

        WorkflowInstanceResponse response = service.pauseInstance(1L);

        assertThat(response.getStatus()).isEqualTo("PAUSED");
    }

    // TC-20: pauseInstance throws WorkflowNotRunningException for non-running instance
    @Test
    void tc20_pauseNonRunningInstanceThrows() {
        WorkflowInstance instance = instanceWithStatus("NEW", "COMPLETED");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.pauseInstance(1L))
                .isInstanceOf(WorkflowNotRunningException.class);
    }

    // TC-21: resumeInstance sets status to RUNNING for paused instance
    @Test
    void tc21_resumeInstanceSetsStatusRunning() {
        WorkflowInstance instance = instanceWithStatus("NEW", "PAUSED");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(inv -> {
            WorkflowInstance wi = inv.getArgument(0);
            if (wi.getUpdatedAt() == null) wi.setUpdatedAt(LocalDateTime.now());
            return wi;
        });
        when(historyRepository.findByWorkflowInstanceOrderByOccurredAtAsc(any()))
                .thenReturn(List.of());

        WorkflowInstanceResponse response = service.resumeInstance(1L);

        assertThat(response.getStatus()).isEqualTo("RUNNING");
    }

    // TC-22: resumeInstance throws WorkflowNotPausedException for non-paused instance
    @Test
    void tc22_resumeNonPausedInstanceThrows() {
        WorkflowInstance instance = runningInstance("NEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.resumeInstance(1L))
                .isInstanceOf(WorkflowNotPausedException.class);
    }

    // TC-23: triggerTransition on PAUSED instance throws WorkflowPausedException
    @Test
    void tc23_triggerOnPausedInstanceThrows() {
        WorkflowInstance instance = instanceWithStatus("NEW", "PAUSED");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.triggerTransition(1L, "SUBMIT_ORDER"))
                .isInstanceOf(WorkflowPausedException.class);
    }

    // Helpers

    private WorkflowDefinition definition() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setName("order-processing");
        def.setStatesJson(STATES_JSON);
        def.setTransitionsJson(TRANSITIONS_JSON);
        def.setCreatedAt(LocalDateTime.now());
        return def;
    }

    private WorkflowInstance runningInstance(String state) {
        return instanceWithStatus(state, "RUNNING");
    }

    private WorkflowInstance instanceWithStatus(String state, String status) {
        WorkflowInstance wi = new WorkflowInstance();
        wi.setId(1L);
        wi.setWorkflowDefinition(definition());
        wi.setCurrentState(state);
        wi.setStatus(status);
        wi.setCreatedAt(LocalDateTime.now());
        wi.setUpdatedAt(LocalDateTime.now());
        return wi;
    }
}
