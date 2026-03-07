package com.example.workflow.repository;

import com.example.workflow.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByName(String name);

    boolean existsByName(String name);
}
