package com.sapphireims.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapphireims.engine.domain.WfDefinition;
import com.sapphireims.engine.dsl.WorkflowSpec;
import com.sapphireims.engine.repo.WfDefinitionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefinitionService {
    private final WfDefinitionRepo repo;
    private final ObjectMapper objectMapper;

    public WfDefinition publish(WorkflowSpec spec, String tenantId) {
        try {
            WfDefinition def = new WfDefinition();
            def.setDefKey(spec.getKey());
            def.setName(spec.getName());
            def.setVersion(spec.getVersion());
            def.setSpecJson(objectMapper.writeValueAsString(spec));
            def.setTenantId(tenantId);
            return repo.save(def);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
