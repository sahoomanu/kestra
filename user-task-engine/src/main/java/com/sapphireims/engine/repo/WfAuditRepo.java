package com.sapphireims.engine.repo;

import com.sapphireims.engine.domain.WfAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WfAuditRepo extends JpaRepository<WfAudit, Long> {
}
