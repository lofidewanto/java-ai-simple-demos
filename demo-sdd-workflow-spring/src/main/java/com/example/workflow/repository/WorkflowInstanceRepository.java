package com.example.workflow.repository;

import com.example.workflow.domain.WorkflowDefinition;
import com.example.workflow.domain.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    List<WorkflowInstance> findByWorkflowDefinition(WorkflowDefinition workflowDefinition);
}
