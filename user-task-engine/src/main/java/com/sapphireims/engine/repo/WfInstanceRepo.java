package com.sapphireims.engine.repo;

import com.sapphireims.engine.domain.WfInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WfInstanceRepo extends JpaRepository<WfInstance, Long> {
}
