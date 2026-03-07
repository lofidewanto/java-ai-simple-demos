package com.example.workflow;

import com.example.workflow.dto.StartWorkflowRequest;
import com.example.workflow.dto.TriggerTransitionRequest;
import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.dto.WorkflowInstanceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-40 through TC-47 — full-stack integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowEngineIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String base() {
        return "http://localhost:" + port;
    }

    // TC-44: Three example workflows loaded at startup
    @Test
    void tc44_threeExampleWorkflowsLoadedAtStartup() {
        ResponseEntity<WorkflowDefinitionDto[]> response =
                restTemplate.getForEntity(base() + "/api/workflow-definitions",
                        WorkflowDefinitionDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);

        List<String> names = List.of(response.getBody()).stream()
                .map(WorkflowDefinitionDto::getName)
                .toList();
        assertThat(names).containsExactlyInAnyOrder(
                "order-processing", "employee-onboarding", "support-ticket");
    }

    // TC-40: Full order-processing happy path
    @Test
    void tc40_fullOrderProcessingHappyPath() {
        Long id = startWorkflow("order-processing");
        assertCurrentState(id, "NEW", "RUNNING");

        trigger(id, "SUBMIT_ORDER");
        assertCurrentState(id, "CHECKING_AVAILABILITY", "RUNNING");

        trigger(id, "STOCK_AVAILABLE");
        assertCurrentState(id, "PAYMENT_PENDING", "RUNNING");

        WorkflowInstanceResponse final_ = trigger(id, "PAYMENT_COLLECTED");
        assertThat(final_.getStatus()).isEqualTo("COMPLETED");
        assertThat(final_.getCurrentState()).isEqualTo("SHIPPED");
    }

    // TC-41: Cancellation path
    @Test
    void tc41_cancellationPath() {
        Long id = startWorkflow("order-processing");
        trigger(id, "SUBMIT_ORDER");
        WorkflowInstanceResponse response = trigger(id, "STOCK_UNAVAILABLE");

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCurrentState()).isEqualTo("CANCELLED");
    }

    // TC-42: Cannot trigger transition after reaching terminal state
    @Test
    void tc42_cannotTriggerAfterTerminalState() {
        Long id = startWorkflow("order-processing");
        trigger(id, "SUBMIT_ORDER");
        trigger(id, "STOCK_UNAVAILABLE"); // → CANCELLED (terminal)

        ResponseEntity<Map> errorResponse = triggerExpectError(id, "SUBMIT_ORDER");
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(errorResponse.getBody()).containsEntry("error", "WORKFLOW_COMPLETED");
    }

    // TC-43: History entries correct count and order after full lifecycle
    @Test
    void tc43_historyEntriesCorrectAfterFullLifecycle() {
        Long id = startWorkflow("order-processing");
        trigger(id, "SUBMIT_ORDER");
        trigger(id, "STOCK_AVAILABLE");
        trigger(id, "PAYMENT_COLLECTED");

        ResponseEntity<WorkflowInstanceResponse.HistoryEntryResponse[]> histResponse =
                restTemplate.getForEntity(base() + "/api/workflow-instances/" + id + "/history",
                        WorkflowInstanceResponse.HistoryEntryResponse[].class);

        assertThat(histResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(histResponse.getBody()).hasSize(4); // START, SUBMIT_ORDER, STOCK_AVAILABLE, PAYMENT_COLLECTED

        // Verify ordering: first entry is START
        assertThat(histResponse.getBody()[0].getAction()).isEqualTo("START");
    }

    // TC-45: Swagger UI accessible
    @Test
    void tc45_swaggerUiIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                base() + "/swagger-ui.html", String.class);
        assertThat(response.getStatusCode().value()).isIn(200, 302);
    }

    // TC-46: H2 console accessible
    @Test
    void tc46_h2ConsoleIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                base() + "/h2-console", String.class);
        assertThat(response.getStatusCode().value()).isIn(200, 302);
    }

    // TC-47: Pause and resume lifecycle
    @Test
    void tc47_pauseAndResumeLifecycle() {
        Long id = startWorkflow("order-processing");
        assertCurrentState(id, "NEW", "RUNNING");

        // Pause
        ResponseEntity<WorkflowInstanceResponse> pauseResp =
                restTemplate.postForEntity(base() + "/api/workflow-instances/" + id + "/pause",
                        null, WorkflowInstanceResponse.class);
        assertThat(pauseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pauseResp.getBody().getStatus()).isEqualTo("PAUSED");

        // Transition should be rejected while paused
        ResponseEntity<Map> errorResp = triggerExpectError(id, "SUBMIT_ORDER");
        assertThat(errorResp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(errorResp.getBody()).containsEntry("error", "WORKFLOW_PAUSED");

        // Resume
        ResponseEntity<WorkflowInstanceResponse> resumeResp =
                restTemplate.postForEntity(base() + "/api/workflow-instances/" + id + "/resume",
                        null, WorkflowInstanceResponse.class);
        assertThat(resumeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resumeResp.getBody().getStatus()).isEqualTo("RUNNING");

        // Continue transitions normally
        WorkflowInstanceResponse after = trigger(id, "SUBMIT_ORDER");
        assertThat(after.getCurrentState()).isEqualTo("CHECKING_AVAILABILITY");
    }

    // ---- helpers ----

    private Long startWorkflow(String workflowName) {
        StartWorkflowRequest req = new StartWorkflowRequest();
        req.setWorkflowName(workflowName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<WorkflowInstanceResponse> response =
                restTemplate.postForEntity(base() + "/api/workflow-instances",
                        new HttpEntity<>(req, headers),
                        WorkflowInstanceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getId();
    }

    private WorkflowInstanceResponse trigger(Long id, String action) {
        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction(action);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<WorkflowInstanceResponse> response =
                restTemplate.exchange(base() + "/api/workflow-instances/" + id + "/transitions",
                        HttpMethod.POST,
                        new HttpEntity<>(req, headers),
                        WorkflowInstanceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<Map> triggerExpectError(Long id, String action) {
        TriggerTransitionRequest req = new TriggerTransitionRequest();
        req.setAction(action);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(base() + "/api/workflow-instances/" + id + "/transitions",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                Map.class);
    }

    private void assertCurrentState(Long id, String expectedState, String expectedStatus) {
        ResponseEntity<WorkflowInstanceResponse> response =
                restTemplate.getForEntity(base() + "/api/workflow-instances/" + id,
                        WorkflowInstanceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCurrentState()).isEqualTo(expectedState);
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus);
    }
}
