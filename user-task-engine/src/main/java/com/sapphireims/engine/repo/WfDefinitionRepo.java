package com.sapphireims.engine.repo;

import com.sapphireims.engine.domain.WfDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WfDefinitionRepo extends JpaRepository<WfDefinition, Long> {
    Optional<WfDefinition> findFirstByDefKeyAndTenantIdOrderByVersionDesc(String defKey, String tenantId);
}
