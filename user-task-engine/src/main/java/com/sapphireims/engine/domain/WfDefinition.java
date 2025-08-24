package com.sapphireims.engine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wf_definition", uniqueConstraints = @UniqueConstraint(name = "uk_def_key_version", columnNames = {"def_key", "version"}))
@Getter
@Setter
public class WfDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "def_key", nullable = false, length = 128)
    private String defKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer version;

    @Lob
    @Column(name = "spec_json", nullable = false)
    private String specJson;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
