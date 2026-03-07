package com.example.workflow.loader;

import com.example.workflow.dto.WorkflowDefinitionDto;
import com.example.workflow.service.WorkflowDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDefinitionLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionLoader.class);

    private final WorkflowDefinitionService definitionService;

    public WorkflowDefinitionLoader(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources("classpath:workflows/*.yml");
        if (resources.length == 0) {
            log.warn("No workflow YAML files found under classpath:workflows/");
            return;
        }

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            try {
                WorkflowDefinitionDto dto = yamlMapper.readValue(
                        resource.getInputStream(), WorkflowDefinitionDto.class);
                definitionService.loadFromDto(dto);
                log.info("Loaded workflow definition: {}", dto.getName());
            } catch (IllegalArgumentException e) {
                log.error("Validation failed for workflow file '{}': {}", filename, e.getMessage());
                throw e; // fail fast — DSL validation errors must not be silently swallowed
            } catch (Exception e) {
                log.error("Failed to load workflow file '{}': {}", filename, e.getMessage());
                throw e;
            }
        }

        log.info("Workflow loader complete: {} definition(s) processed", resources.length);
    }
}
