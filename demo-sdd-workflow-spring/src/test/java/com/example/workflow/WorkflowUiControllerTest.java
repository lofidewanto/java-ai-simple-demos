package com.example.workflow;

import com.example.workflow.controller.WorkflowUiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * TC-UI-01 through TC-UI-04 — @WebMvcTest for WorkflowUiController.
 */
@WebMvcTest(controllers = WorkflowUiController.class)
class WorkflowUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // TC-UI-01: GET /ui redirects to /ui/workflows
    @Test
    void tcUI01_rootRedirectsToWorkflows() throws Exception {
        mockMvc.perform(get("/ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/workflows"));
    }

    // TC-UI-02: GET /ui/workflows returns 200 and renders "workflows" view
    @Test
    void tcUI02_workflowsPageReturns200() throws Exception {
        mockMvc.perform(get("/ui/workflows"))
                .andExpect(status().isOk())
                .andExpect(view().name("workflows"));
    }

    // TC-UI-03: GET /ui/instances returns 200 and renders "instances" view
    @Test
    void tcUI03_instancesPageReturns200() throws Exception {
        mockMvc.perform(get("/ui/instances"))
                .andExpect(status().isOk())
                .andExpect(view().name("instances"));
    }

    // TC-UI-04: GET /ui/instances/{id} returns 200 and renders "instance-detail" view
    @Test
    void tcUI04_instanceDetailPageReturns200() throws Exception {
        mockMvc.perform(get("/ui/instances/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("instance-detail"));
    }
}
