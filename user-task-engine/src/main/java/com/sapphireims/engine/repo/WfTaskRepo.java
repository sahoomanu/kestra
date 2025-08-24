package com.sapphireims.engine.repo;

import com.sapphireims.engine.domain.WfTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WfTaskRepo extends JpaRepository<WfTask, Long> {
    @Query("select t from WfTask t where t.status='READY' and t.tenantId=:tenant and (t.assignee=:user or t.candidateGroups like %:group%)")
    List<WfTask> findReadyForUserOrGroup(@Param("user") String user, @Param("group") String group, @Param("tenant") String tenant);
}
