package com.sapphireims.engine.web;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskView {
    private Long id;
    private Long wfInstanceId;
    private String nodeId;
    private String name;
    private String assignee;
    private String candidateGroups;
    private LocalDateTime dueDate;
    private LocalDateTime followUpDate;
    private Integer priority;
    private String status;
}
