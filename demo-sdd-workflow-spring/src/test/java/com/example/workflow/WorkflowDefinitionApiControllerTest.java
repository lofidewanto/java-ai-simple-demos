package com.example.workflow;

import com.example.workflow.dto.StateDto;
import com.example.workflow.dto.TransitionDto;
import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.exception.WorkflowNotFoundException;
import com.example.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-24 through TC-26
 */
@WebMvcTest(controllers = com.example.workflow.controller.WorkflowDefinitionApiController.class)
class WorkflowDefinitionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkflowDefinitionService service;

    // TC-24: GET /api/workflow-definitions returns 200 with list
    @Test
    void tc24_listAllReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(orderProcessingDto()));

        mockMvc.perform(get("/api/workflow-definitions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("order-processing"));
    }

    // TC-25: GET /api/workflow-definitions/{name} returns 200 for known name
    @Test
    void tc25_getByNameReturns200() throws Exception {
        when(service.findByName("order-processing")).thenReturn(orderProcessingDto());

        mockMvc.perform(get("/api/workflow-definitions/order-processing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("order-processing"));
    }

    // TC-26: GET /api/workflow-definitions/{name} returns 404 for unknown name
    @Test
    void tc26_getByUnknownNameReturns404() throws Exception {
        when(service.findByName("unknown")).thenThrow(new WorkflowNotFoundException("unknown"));

        mockMvc.perform(get("/api/workflow-definitions/unknown")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WORKFLOW_NOT_FOUND"));
    }

    private WorkflowDefinitionDto orderProcessingDto() {
        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        dto.setName("order-processing");

        StateDto s = new StateDto();
        s.setName("NEW");
        s.setInitial(true);
        dto.setStates(List.of(s));

        TransitionDto t = new TransitionDto();
        t.setFrom("NEW");
        t.setTo("DONE");
        t.setAction("GO");
        dto.setTransitions(List.of(t));

        return dto;
    }
}
