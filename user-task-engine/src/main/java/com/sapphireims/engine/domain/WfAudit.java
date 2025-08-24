package com.sapphireims.engine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wf_audit")
@Getter
@Setter
public class WfAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wf_instance_id", nullable = false)
    private WfInstance wfInstance;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "node_id")
    private String nodeId;

    @Column(nullable = false, length = 64)
    private String event;

    private String message;

    @Lob
    @Column(name = "data_json")
    private String dataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
