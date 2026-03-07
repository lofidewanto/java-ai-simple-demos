package com.example.workflow.repository;

import com.example.workflow.domain.WorkflowHistoryEntry;
import com.example.workflow.domain.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowHistoryEntryRepository extends JpaRepository<WorkflowHistoryEntry, Long> {

    List<WorkflowHistoryEntry> findByWorkflowInstanceOrderByOccurredAtAsc(WorkflowInstance instance);
}
