package com.sapphireims.engine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wf_task")
@Getter
@Setter
public class WfTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wf_instance_id", nullable = false)
    private WfInstance wfInstance;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String assignee;

    @Column(name = "candidate_groups")
    private String candidateGroups;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    private Integer priority;

    @Column(nullable = false, length = 32)
    private String status;

    @Lob
    @Column(name = "input_json")
    private String inputJson;

    @Lob
    @Column(name = "output_json")
    private String outputJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
