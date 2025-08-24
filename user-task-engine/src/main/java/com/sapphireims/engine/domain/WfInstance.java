package com.sapphireims.engine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wf_instance")
@Getter
@Setter
public class WfInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "def_id", nullable = false)
    private WfDefinition definition;

    @Column(name = "business_key")
    private String businessKey;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Lob
    @Column(name = "variables_json", nullable = false)
    private String variablesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
