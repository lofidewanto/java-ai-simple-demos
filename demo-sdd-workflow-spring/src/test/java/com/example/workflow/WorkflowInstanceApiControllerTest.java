package com.example.workflow;

import com.example.workflow.dto.StartWorkflowRequest;
import com.example.workflow.dto.TriggerTransitionRequest;
import com.example.workflow.dto.WorkflowInstanceResponse;
import com.example.workflow.exception.InstanceNotFoundException;
import com.example.workflow.exception.InvalidTransitionException;
import com.example.workflow.exception.WorkflowCompletedException;
import com.example.workflow.exception.WorkflowNotFoundException;
import com.example.workflow.exception.WorkflowNotPausedException;
import com.example.workflow.exception.WorkflowNotRunningException;
import com.example.workflow.exception.WorkflowPausedException;
import com.example.workflow.service.WorkflowInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-27 through TC-39
 */
@WebMvcTest(controllers = com.example.workflow.controller.WorkflowInstanceApiController.class)
class WorkflowInstanceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowInstanceService service;

    // TC-27: POST /api/workflow-instances returns 201 with new instance
    @Test
    void tc27_startInstanceReturns201() throws Exception {
        when(service.startInstance("order-processing")).thenReturn(runningResponse("NEW"));

        StartWorkflowRequest req = new StartWorkflowRequest();
        req.setWorkflowName("order-processing");

        mockMvc.perform(post("/api/workflow-instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentState").value("NEW"));
    }

    // TC-28: POST with unknown workflowName returns 404
    @Test
    void tc28_startWithUnknownWorkflowNameReturns404() throws Exception {
        when(service.startInstance("ghost")).thenThrow(new WorkflowNotFoundException("ghost"));

        StartWorkflowRequest req = new StartWorkflowRequest();
        req.setWorkflowName("ghost");

        mockMvc.perform(post("/api/workflow-instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WORKFLOW_NOT_FOUND"));
    }

    // TC-29: GET /api/workflow-instances/{id} returns 200 with history
    @Test
    void tc29_getByIdReturns200WithHistory() throws Exception {
        WorkflowInstanceResponse response = runningResponse("NEW");
        WorkflowInstanceResponse.HistoryEntryResponse h = new WorkflowInstanceResponse.HistoryEntryResponse();
        h.setFromState(null);
        h.setToState("NEW");
        h.setAction("START");
        h.setTaskType("HUMAN");
        h.setOccurredAt(LocalDateTime.now());
        response.setHistory(List.of(h));

        when(service.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/workflow-instances/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].action").value("START"));
    }

    // TC-30: GET /api/workflow-instances/{id} returns 404 for unknown ID
    @Test
    void tc30_getByUnknownIdReturns404() throws Exception {
        when(service.findById(99L)).thenThrow(new InstanceNotFoundException(99L));

        mockMvc.perform(get("/api/workflow-instances/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INSTANCE_NOT_FOUND"));
    }

    // TC-31: POST /api/workflow-instances/{id}/transitions returns 200 on valid action
    @Test
    void tc31_triggerValidActionReturns200() throws Exception {
        when(service.triggerTransition(eq(1L), eq("SUBMIT_ORDER")))
                .thenReturn(runningResponse("CHECKING_AVAILABILITY"));

        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction("SUBMIT_ORDER");

        mockMvc.perform(post("/api/workflow-instances/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("CHECKING_AVAILABILITY"));
    }

    // TC-32: POST transitions returns 422 on invalid action
    @Test
    void tc32_triggerInvalidActionReturns422() throws Exception {
        when(service.triggerTransition(eq(1L), eq("BAD_ACTION")))
                .thenThrow(new InvalidTransitionException("BAD_ACTION", "NEW"));

        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction("BAD_ACTION");

        mockMvc.perform(post("/api/workflow-instances/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // TC-33: POST transitions returns 422 on completed instance
    @Test
    void tc33_triggerOnCompletedInstanceReturns422() throws Exception {
        when(service.triggerTransition(eq(1L), any()))
                .thenThrow(new WorkflowCompletedException(1L, "SHIPPED"));

        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction("SUBMIT_ORDER");

        mockMvc.perform(post("/api/workflow-instances/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WORKFLOW_COMPLETED"));
    }

    // TC-34: POST transitions returns 422 on paused instance
    @Test
    void tc34_triggerOnPausedInstanceReturns422() throws Exception {
        when(service.triggerTransition(eq(1L), any()))
                .thenThrow(new WorkflowPausedException(1L));

        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction("SUBMIT_ORDER");

        mockMvc.perform(post("/api/workflow-instances/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WORKFLOW_PAUSED"));
    }

    // TC-35: POST /api/workflow-instances/{id}/pause returns 200 on running instance
    @Test
    void tc35_pauseRunningInstanceReturns200() throws Exception {
        WorkflowInstanceResponse paused = runningResponse("NEW");
        paused.setStatus("PAUSED");
        when(service.pauseInstance(1L)).thenReturn(paused);

        mockMvc.perform(post("/api/workflow-instances/1/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    // TC-36: POST /api/workflow-instances/{id}/pause returns 422 on non-running instance
    @Test
    void tc36_pauseNonRunningInstanceReturns422() throws Exception {
        when(service.pauseInstance(1L)).thenThrow(new WorkflowNotRunningException(1L, "COMPLETED"));

        mockMvc.perform(post("/api/workflow-instances/1/pause"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WORKFLOW_NOT_RUNNING"));
    }

    // TC-37: POST /api/workflow-instances/{id}/resume returns 200 on paused instance
    @Test
    void tc37_resumePausedInstanceReturns200() throws Exception {
        when(service.resumeInstance(1L)).thenReturn(runningResponse("NEW"));

        mockMvc.perform(post("/api/workflow-instances/1/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    // TC-38: POST /api/workflow-instances/{id}/resume returns 422 on non-paused instance
    @Test
    void tc38_resumeNonPausedInstanceReturns422() throws Exception {
        when(service.resumeInstance(1L)).thenThrow(new WorkflowNotPausedException(1L, "RUNNING"));

        mockMvc.perform(post("/api/workflow-instances/1/resume"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WORKFLOW_NOT_PAUSED"));
    }

    // TC-39: GET /api/workflow-instances?workflowName=X filters correctly
    @Test
    void tc39_filterByWorkflowNameReturnsMatchingInstances() throws Exception {
        WorkflowInstanceResponse r = runningResponse("NEW");
        when(service.findAll("order-processing")).thenReturn(List.of(r));

        mockMvc.perform(get("/api/workflow-instances")
                        .param("workflowName", "order-processing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workflowName").value("order-processing"));
    }

    // Helper

    private WorkflowInstanceResponse runningResponse(String state) {
        WorkflowInstanceResponse r = new WorkflowInstanceResponse();
        r.setId(1L);
        r.setWorkflowName("order-processing");
        r.setCurrentState(state);
        r.setStatus("RUNNING");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setHistory(List.of());
        return r;
    }
}
